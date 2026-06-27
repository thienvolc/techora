package com.techora.inventory.domain.event;

import com.techora.common.domain.event.InternalEvent;

import java.time.Instant;
import java.util.UUID;

public record StockReducedEvent(
        UUID productId,
        int quantity,
        int quantityOnHand,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return productId;
    }

    public static StockReducedEvent of(UUID productId,
                                       int quantity,
                                       int quantityOnHand,
                                       Instant occurredAt) {
        return new StockReducedEvent(
                productId,
                quantity,
                quantityOnHand,
                occurredAt
        );
    }
}
