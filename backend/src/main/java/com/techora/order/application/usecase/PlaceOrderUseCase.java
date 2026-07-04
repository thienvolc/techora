package com.techora.order.application.usecase;

import com.techora.cart.CartService;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.idempotency.IdempotencyCommandExecutor;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.mapper.PlaceOrderMapper;
import com.techora.order.application.model.OrderPrice;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.port.inventory.OrderInventoryPort;
import com.techora.order.application.port.payment.InitiatedOrderPayment;
import com.techora.order.application.port.payment.PaymentInitiationPort;
import com.techora.order.application.service.OrderCreator;
import com.techora.order.application.service.OrderPricingService;
import com.techora.order.application.service.OrderStatusService;
import com.techora.order.application.service.PlaceOrderIdempotencyFactory;
import com.techora.order.domain.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final OrderCreator orderCreator;
    private final OrderStatusService orderStatusService;
    private final OrderPricingService orderPricingService;
    private final CartService cartService;

    private final OrderInventoryPort orderInventoryPort;
    private final PaymentInitiationPort paymentInitiationPort;

    private final IdempotencyCommandExecutor idempotencyCommandExecutor;
    private final PlaceOrderIdempotencyFactory placeOrderIdempotencyFactory;

    private final PlaceOrderMapper mapper;

    // INFO: We "can" use the wrapped Transaction annotation,
    // because the Network I/O calls is not exists in current inter-module (ports)
    @Transactional
    public PlaceOrderView execute(PlaceOrderCommand command) {
        return idempotencyCommandExecutor.execute(
                placeOrderIdempotencyFactory.create(command),
                () -> placeOrder(command)
        );
    }

    private PlaceOrderView placeOrder(PlaceOrderCommand placeOrderCmd) {
        CartSnapshot cart = cartService.getPlaceOrderCartSnapshot(placeOrderCmd.userId());
        validateCartHasItems(cart);
        Order placedOrder = createOrderFromCart(cart);
        Order stockReservedOrder = reserveInventory(placedOrder);
        InitiatedOrderPayment payment = initiatePayment(stockReservedOrder, placeOrderCmd);
        cartService.clearCart(placeOrderCmd.userId());
        return mapper.toPlaceOrderView(stockReservedOrder, payment);
    }

    public Order createOrderFromCart(CartSnapshot cart) {
        OrderPrice orderPrice = orderPricingService.calculate(cart);
        return orderCreator.create(mapper.toCreateOrderCommand(cart, orderPrice));
    }

    private Order reserveInventory(Order placedOrder) {
        orderInventoryPort.reserve(mapper.toReserveInventoryCommand(placedOrder));
        return orderStatusService.markStockReserved(placedOrder.getId());
    }

    private InitiatedOrderPayment initiatePayment(Order stockReservedOrder, PlaceOrderCommand placeOrderCmd) {
        return paymentInitiationPort.initiate(
                mapper.toInitiateOrderPaymentCommand(stockReservedOrder, placeOrderCmd));
    }

    private void validateCartHasItems(CartSnapshot cart) {
        if (cart.isEmpty()) {
            throw new BusinessException(ResponseCode.CART_EMPTY);
        }
    }
}
