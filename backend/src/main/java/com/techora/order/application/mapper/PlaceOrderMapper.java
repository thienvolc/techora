package com.techora.order.application.mapper;

import com.techora.cart.dto.order.CartItemSnapshot;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.order.application.command.CreateOrderCommand;
import com.techora.order.application.command.CreateOrderItemCommand;
import com.techora.order.application.model.OrderPrice;
import com.techora.order.application.model.PlaceOrderItemView;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.port.inventory.ReserveOrderInventoryCommand;
import com.techora.order.application.port.inventory.ReserveOrderInventoryItemCommand;
import com.techora.order.application.port.payment.InitiatedOrderPayment;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderItem;
import com.techora.order.domain.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PlaceOrderMapper {

    public Order toOrder(CreateOrderCommand command, Instant createdAt, Instant paymentDeadlineAt) {
        return Order.builder()
                .userId(command.userId())
                .username(command.username())
                .status(OrderStatus.CREATED)
                .total(command.total())
                .paymentDeadlineAt(paymentDeadlineAt)
                .items(command.items().stream()
                        .map(this::toOrderItem)
                        .toList())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private OrderItem toOrderItem(CreateOrderItemCommand item) {
        return OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName())
                .sku(item.sku())
                .unitPrice(item.unitPrice())
                .quantity(item.quantity())
                .subtotal(item.subtotal())
                .build();
    }

    public PlaceOrderView toPlaceOrderView(Order order, InitiatedOrderPayment payment) {
        return new PlaceOrderView(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotal(),
                payment.paymentId(),
                payment.paymentUrl(),
                payment.expiresAt(),
                order.getItems().stream().map(this::toPlaceOrderItemView).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private PlaceOrderItemView toPlaceOrderItemView(OrderItem item) {
        return new PlaceOrderItemView(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    public CreateOrderCommand toCreateOrderCommand(CartSnapshot cart, OrderPrice orderPrice) {
        return new CreateOrderCommand(
                cart.userId(),
                cart.username(),
                orderPrice.total(),
                cart.items().stream()
                        .map(item -> toCreateOrderItemCommand(item, orderPrice))
                        .toList()
        );
    }

    private CreateOrderItemCommand toCreateOrderItemCommand(CartItemSnapshot cartItem, OrderPrice orderPrice) {
        var subtotal = orderPrice.subtotalOf(cartItem.productId());

        return new CreateOrderItemCommand(
                cartItem.productId(),
                cartItem.productName(),
                cartItem.sku(),
                cartItem.unitPrice(),
                cartItem.quantity(),
                subtotal
        );
    }

    public ReserveOrderInventoryCommand toReserveInventoryCommand(Order order, Instant expiresAt) {
        return new ReserveOrderInventoryCommand(
                order.getId(),
                expiresAt,
                order.getItems().stream()
                        .map(this::toReserveInventoryItemCommand)
                        .toList()
        );
    }

    private ReserveOrderInventoryItemCommand toReserveInventoryItemCommand(OrderItem item) {
        return new ReserveOrderInventoryItemCommand(
                item.getProductId(),
                item.getQuantity()
        );
    }
}
