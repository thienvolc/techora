package com.techora.order.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID userId,
        String username,
        OrderStatus status,
        BigDecimal total,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
