package com.techora.catalog.projection.event;

import com.techora.common.domain.event.InternalEvent;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ProductProjectionDeletedEvent(
        UUID productId,
        @Nullable String slug,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return productId;
    }

    public static ProductProjectionDeletedEvent of(UUID productId) {
        return new ProductProjectionDeletedEvent(productId, null, Instant.now());
    }

    public static ProductProjectionDeletedEvent of(UUID productId, String slug) {
        return new ProductProjectionDeletedEvent(productId, slug, Instant.now());
    }
}
