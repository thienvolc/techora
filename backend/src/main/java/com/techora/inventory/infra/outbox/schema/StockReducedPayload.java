package com.techora.inventory.infra.outbox.schema;

import com.techora.inventory.domain.event.StockReducedEvent;

import java.util.UUID;

public record StockReducedPayload(
        int eventVersion,
        UUID productId,
        int quantity,
        int stockQuantity
) implements InventoryEventPayload {

    public static StockReducedPayload from(StockReducedEvent event) {
        return new StockReducedPayload(
                event.eventVersion(),
                event.productId(),
                event.quantity(),
                event.quantityOnHand()
        );
    }
}
