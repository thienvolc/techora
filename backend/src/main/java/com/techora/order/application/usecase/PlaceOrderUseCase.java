package com.techora.order.application.usecase;

import com.techora.cart.CartService;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.idempotency.IdempotencyCommandExecutor;
import com.techora.order.application.command.CreateOrderCommand;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.mapper.PlaceOrderMapper;
import com.techora.order.application.model.OrderPrice;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.port.inventory.OrderInventoryPort;
import com.techora.order.application.port.payment.PaymentInitiationPort;
import com.techora.order.application.port.payment.InitiateOrderPaymentCommand;
import com.techora.order.application.port.payment.InitiatedOrderPayment;
import com.techora.order.application.service.OrderCreator;
import com.techora.order.application.service.OrderPricingService;
import com.techora.order.application.service.OrderStatusService;
import com.techora.order.application.service.PlaceOrderIdempotencyFactory;
import com.techora.order.domain.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final CartService cartService;
    private final OrderPricingService orderPricingService;

    private final OrderInventoryPort orderInventoryPort;
    private final PaymentInitiationPort paymentInitiationPort;

    private final IdempotencyCommandExecutor idempotencyCommandExecutor;
    private final PlaceOrderIdempotencyFactory placeOrderIdempotencyFactory;
    private final PlaceOrderMapper placeOrderMapper;
    private final OrderCreator orderCreator;
    private final OrderStatusService orderStatusService;

    @Transactional
    public PlaceOrderView execute(PlaceOrderCommand command) {
        return idempotencyCommandExecutor.execute(
                placeOrderIdempotencyFactory.create(command),
                () -> handleWithoutIdempotency(command)
        );
    }

    private PlaceOrderView handleWithoutIdempotency(PlaceOrderCommand command) {
        UUID userId = command.userId();
        CartSnapshot cart = cartService.getPlaceOrderCartSnapshot(userId);
        validateCartHasItems(cart);

        OrderPrice orderPrice = orderPricingService.calculate(cart);
        CreateOrderCommand createOrderCommand = placeOrderMapper.toCreateOrderCommand(cart, orderPrice);
        Order placedOrder = orderCreator.create(createOrderCommand);

        orderInventoryPort.reserve(
                placeOrderMapper.toReserveInventoryCommand(placedOrder, placedOrder.getPaymentDeadlineAt()));

        Order stockReservedOrder = orderStatusService.markStockReserved(placedOrder.getId());
        InitiatedOrderPayment payment = paymentInitiationPort.initiate(new InitiateOrderPaymentCommand(
                userId,
                stockReservedOrder.getId(),
                stockReservedOrder.getPaymentDeadlineAt(),
                command.ipAddress(),
                command.idempotencyKey()
        ));

        cartService.clearCart(userId);

        return placeOrderMapper.toPlaceOrderView(stockReservedOrder, payment);
    }

    private void validateCartHasItems(CartSnapshot cart) {
        if (cart.isEmpty()) {
            throw new BusinessException(ResponseCode.CART_EMPTY);
        }
    }
}
