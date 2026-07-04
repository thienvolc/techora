package com.techora.order.infra.outbox.schema;

import com.techora.order.domain.event.OrderPlacedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedPayload(
        int eventVersion,
        UUID orderId,
        UUID userId,
        BigDecimal total,
        Instant occurredAt
) implements OrderEventPayload {

    public static OrderPlacedPayload fromEvent(OrderPlacedEvent event) {
        return new OrderPlacedPayload(
                event.eventVersion(),
                event.orderId(),
                event.userId(),
                event.total(),
                event.occurredAt()
        );
    }
}
