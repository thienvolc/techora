package com.techora.checkout.application;

import com.techora.cart.dto.checkout.CartCheckoutItem;
import com.techora.cart.dto.checkout.CartCheckoutSnapshot;
import com.techora.checkout.controller.response.CheckoutItemResponse;
import com.techora.checkout.controller.response.CheckoutResponse;
import com.techora.checkout.application.port.payment.CheckoutPaymentResult;
import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.command.PlaceOrderItemCommand;
import com.techora.order.application.result.OrderItemSnapshot;
import com.techora.order.application.result.OrderSnapshot;
import com.techora.order.application.service.OrderPrice;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CheckoutMapper {

    public CheckoutResponse toResponse(OrderSnapshot order) {
        return new CheckoutResponse(
                order.orderId(),
                order.userId(),
                order.status(),
                order.total(),
                null,
                null,
                null,
                order.items().stream().map(this::toItemResponse).toList(),
                order.createdAt(),
                order.updatedAt()
        );
    }

    public CheckoutResponse toResponse(OrderSnapshot order, CheckoutPaymentResult payment) {
        return new CheckoutResponse(
                order.orderId(),
                order.userId(),
                payment.orderStatus(),
                order.total(),
                payment.paymentId(),
                payment.paymentUrl(),
                payment.expiresAt(),
                order.items().stream().map(this::toItemResponse).toList(),
                order.createdAt(),
                order.updatedAt()
        );
    }

    public PlaceOrderCommand toPlaceOrderCommand(CartCheckoutSnapshot cart, OrderPrice orderPrice) {
        return new PlaceOrderCommand(
                cart.userId(),
                cart.username(),
                orderPrice.total(),
                cart.items().stream()
                        .map(item -> toPlaceOrderItemCommand(item, orderPrice))
                        .toList()
        );
    }

    public ReserveInventoryCommand toReserveInventoryCommand(OrderSnapshot order, Instant expiresAt) {
        return new ReserveInventoryCommand(
                order.orderId(),
                expiresAt,
                order.items().stream()
                        .map(this::toReserveInventoryItem)
                        .toList()
        );
    }

    private CheckoutItemResponse toItemResponse(OrderItemSnapshot item) {
        return new CheckoutItemResponse(
                item.itemId(),
                item.productId(),
                item.productName(),
                item.sku(),
                item.unitPrice(),
                item.quantity(),
                item.subtotal()
        );
    }

    private PlaceOrderItemCommand toPlaceOrderItemCommand(CartCheckoutItem cartItem, OrderPrice orderPrice) {
        var subtotal = orderPrice.subtotalOf(cartItem.productId());

        return new PlaceOrderItemCommand(
                cartItem.productId(),
                cartItem.productName(),
                cartItem.sku(),
                cartItem.unitPrice(),
                cartItem.quantity(),
                subtotal
        );
    }

    private ReserveInventoryItem toReserveInventoryItem(OrderItemSnapshot item) {
        return new ReserveInventoryItem(
                item.productId(),
                item.quantity()
        );
    }
}
