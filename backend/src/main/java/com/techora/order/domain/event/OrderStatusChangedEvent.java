package com.techora.order.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
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

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
