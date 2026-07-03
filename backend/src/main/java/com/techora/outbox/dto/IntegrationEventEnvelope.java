package com.techora.outbox.dto;

import java.time.Instant;
import java.util.UUID;

public record IntegrationEventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        T data
) {
    public static <T> IntegrationEventEnvelope<T> from(UUID eventId, OutboxEventRecord<T> record) {
        return new IntegrationEventEnvelope<>(
                eventId,
                record.eventType().name(),
                record.eventVersion(),
                record.occurredAt(),
                record.data()
        );
    }
}
