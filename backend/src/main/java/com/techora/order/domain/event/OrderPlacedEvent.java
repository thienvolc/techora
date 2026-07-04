package com.techora.order.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        int eventVersion,
        UUID orderId,
        UUID userId,
        String username,
        OrderStatus status,
        BigDecimal total,
        Instant occurredAt
) implements InternalEvent {

    private static final int CURRENT_VERSION = 1;

    @Override
    public UUID aggregateId() {
        return orderId;
    }

    public static OrderPlacedEvent from(Order order) {
        return new OrderPlacedEvent(
                CURRENT_VERSION,
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt()
        );
    }
}
