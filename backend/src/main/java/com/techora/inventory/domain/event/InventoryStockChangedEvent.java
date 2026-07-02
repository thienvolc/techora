package com.techora.inventory.domain.event;

import com.techora.common.domain.event.InternalEvent;

import java.time.Instant;
import java.util.UUID;

public record InventoryStockChangedEvent(
        UUID productId,
        int availableQuantity,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return productId;
    }

    public static InventoryStockChangedEvent of(UUID productId,
                                                int availableQuantity,
                                                Instant occurredAt) {
        return new InventoryStockChangedEvent(productId, availableQuantity, occurredAt);
    }
}
