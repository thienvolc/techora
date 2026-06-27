package com.techora.catalog.projection.event;

import com.techora.common.domain.event.InternalEvent;

import java.time.Instant;
import java.util.UUID;

public record CategoryProjectionChangedEvent(
        UUID categoryId,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return categoryId;
    }

    public static CategoryProjectionChangedEvent of(UUID categoryId) {
        return new CategoryProjectionChangedEvent(categoryId, Instant.now());
    }
}
