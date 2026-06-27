package com.techora.order.application.result;

import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshot(
        UUID orderId,
        UUID userId,
        String username,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemSnapshot> items,
        Instant createdAt,
        Instant updatedAt
) {
    public OrderSnapshot {
        items = List.copyOf(items);
    }

    public static OrderSnapshot from(Order order) {
        return new OrderSnapshot(
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getStatus(),
                order.getTotal(),
                order.getItems().stream()
                        .map(OrderItemSnapshot::from)
                        .toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
