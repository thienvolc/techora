package com.techora.outbox.dto;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder
public record OutboxEventRecord<T>(
        OutboxAggregateType aggregateType,
        UUID aggregateId,
        OutboxEventType eventType,
        String topic,
        String messageKey,
        int eventVersion,
        Instant occurredAt,
        OutboxHeaders headers,
        T data
) {
    private static final int DEFAULT_EVENT_VERSION = 1;

    public OutboxEventRecord {
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        messageKey = StringUtils.hasText(messageKey) ? messageKey : aggregateId.toString();
        eventVersion = eventVersion <= 0 ? DEFAULT_EVENT_VERSION : eventVersion;
    }
}
