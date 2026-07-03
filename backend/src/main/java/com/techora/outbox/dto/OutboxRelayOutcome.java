package com.techora.outbox.dto;

import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.constant.OutboxRelayOutcomeType;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record OutboxRelayOutcome(
        UUID eventId,
        OutboxEventType eventType,
        OutboxRelayOutcomeType type,
        @Nullable String errorMessage,
        Instant nextAttemptAt,
        Instant occurredAt
) {
    public static OutboxRelayOutcome published(UUID eventId,
                                               OutboxEventType eventType,
                                               Instant now) {
        return new OutboxRelayOutcome(
                eventId,
                eventType,
                OutboxRelayOutcomeType.PUBLISHED,
                null,
                null,
                now
        );
    }

    public static OutboxRelayOutcome retryScheduled(UUID eventId,
                                                    OutboxEventType eventType,
                                                    String errorMessage,
                                                    Instant nextAttemptAt,
                                                    Instant now) {

        return new OutboxRelayOutcome(
                eventId,
                eventType,
                OutboxRelayOutcomeType.RETRY_SCHEDULED,
                errorMessage,
                nextAttemptAt,
                now
        );
    }

    public static OutboxRelayOutcome failed(UUID eventId,
                                            OutboxEventType eventType,
                                            String errorMessage,
                                            Instant now) {

        return new OutboxRelayOutcome(
                eventId,
                eventType,
                OutboxRelayOutcomeType.FAILED,
                errorMessage,
                null,
                now
        );
    }
}
