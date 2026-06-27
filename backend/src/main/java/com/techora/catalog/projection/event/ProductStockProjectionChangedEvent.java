package com.techora.catalog.projection.event;

import com.techora.common.domain.event.InternalEvent;

import java.time.Instant;
import java.util.UUID;

public record ProductStockProjectionChangedEvent(
        UUID productId,
        int stockQuantity,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return productId;
    }

    public static ProductStockProjectionChangedEvent of(UUID productId,
                                                        int stockQuantity,
                                                        Instant occurredAt) {
        return new ProductStockProjectionChangedEvent(productId, stockQuantity, occurredAt);
    }
}
