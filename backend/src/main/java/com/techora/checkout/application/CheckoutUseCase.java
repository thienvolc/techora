package com.techora.checkout.application;

import com.techora.common.application.aop.BusinessException;
import com.techora.cart.CartService;
import com.techora.cart.dto.checkout.CartCheckoutSnapshot;
import com.techora.checkout.controller.response.CheckoutResponse;
import com.techora.common.application.constant.ResponseCode;
import com.techora.idempotency.IdempotencyCommandExecutor;
import com.techora.inventory.application.service.InventoryReservationService;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.service.OrderCheckoutService;
import com.techora.order.application.service.OrderPrice;
import com.techora.order.application.result.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutUseCase {
    private final CheckoutMapper checkoutMapper;
    private final CartService cartService;
    private final OrderPricingService orderPricingService;
    private final OrderCheckoutService orderCheckoutService;
    private final InventoryReservationService inventoryReservationService;
    private final IdempotencyCommandExecutor idempotencyCommandExecutor;
    private final CheckoutIdempotencyFactory checkoutIdempotencyFactory;

    @Transactional
    public CheckoutResponse execute(CheckoutCommand command) {
        return idempotencyCommandExecutor.execute(
                checkoutIdempotencyFactory.create(command),
                () -> handleWithoutIdempotency(command.userId())
        );
    }

    private CheckoutResponse handleWithoutIdempotency(UUID userId) {
        CartCheckoutSnapshot cart = cartService.getCheckoutSnapshot(userId);
        validateCartHasItems(cart);

        OrderPrice orderPrice = orderPricingService.calculate(cart);
        PlaceOrderCommand placeOrderCommand = checkoutMapper.toPlaceOrderCommand(cart, orderPrice);
        OrderSnapshot placedOrder = orderCheckoutService.place(placeOrderCommand);

        inventoryReservationService.reserve(
                checkoutMapper.toReserveInventoryCommand(placedOrder));
        OrderSnapshot stockReservedOrder = orderCheckoutService.markStockReserved(placedOrder.orderId());

        cartService.clearCart(userId);

        return checkoutMapper.toResponse(stockReservedOrder);
    }

    private void validateCartHasItems(CartCheckoutSnapshot cart) {
        if (cart.isEmpty()) {
            throw new BusinessException(ResponseCode.CART_EMPTY);
        }
    }

}
