package com.techora.order.infra.outbox.schema;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.domain.event.OrderStatusChangedEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedPayload(
        int eventVersion,
        UUID orderId,
        UUID userId,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        Instant occurredAt
) implements OrderEventPayload {

    public static OrderStatusChangedPayload from(OrderStatusChangedEvent event) {
        return new OrderStatusChangedPayload(
                event.eventVersion(),
                event.orderId(),
                event.userId(),
                event.oldStatus(),
                event.newStatus(),
                event.occurredAt()
        );
    }
}
