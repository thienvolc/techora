package com.techora.inventory.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.inventory.domain.entity.InventoryItemEntity;

import java.time.Instant;
import java.util.UUID;

public record StockReducedEvent(
        int eventVersion,
        UUID productId,
        int quantity,
        int quantityOnHand,
        Instant occurredAt
) implements InternalEvent {

    private static final int CURRENT_VERSION = 1;

    @Override
    public UUID aggregateId() {
        return productId;
    }

    public static InternalEvent from(InventoryItemEntity item, int quantity) {
        return new StockReducedEvent(
                CURRENT_VERSION,
                item.getProductId(),
                quantity,
                item.getQuantityOnHand(),
                item.getUpdatedAt()
        );
    }
}
