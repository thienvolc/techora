package com.techora.catalog.projection.event;

import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.common.domain.event.InternalEvent;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ProductProjectionChangedEvent(
        ProductProjectionSnapshot product,
        @Nullable String previousSlug,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return product.id();
    }

    public static ProductProjectionChangedEvent of(ProductProjectionSnapshot product) {
        return new ProductProjectionChangedEvent(product, null, Instant.now());
    }

    public static ProductProjectionChangedEvent of(ProductProjectionSnapshot product,
                                                   String previousSlug) {
        return new ProductProjectionChangedEvent(product, previousSlug, Instant.now());
    }
}
