package com.techora.order.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        int eventVersion,
        UUID orderId,
        UUID userId,
        String username,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        UUID actorId,
        String actorName,
        OrderEventActorType actorType,
        Instant occurredAt
) implements InternalEvent {

    private static final int CURRENT_VERSION = 1;

    @Override
    public UUID aggregateId() {
        return orderId;
    }

    public OrderStatusChangedEvent(
            UUID orderId,
            UUID userId,
            String username,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            UUID actorId,
            String actorName,
            OrderEventActorType actorType,
            Instant occurredAt
    ) {
        this(
                CURRENT_VERSION,
                orderId,
                userId,
                username,
                oldStatus,
                newStatus,
                actorId,
                actorName,
                actorType,
                occurredAt
        );
    }
}
