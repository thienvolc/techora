package com.techora.outbox.dto;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OutboxEventRecord(
        OutboxAggregateType aggregateType,
        UUID aggregateId,
        OutboxEventType eventType,
        String topic,
        String messageKey,
        int eventVersion,
        Map<String, String> headers,
        Map<String, Object> attributes
) {
    private static final int DEFAULT_EVENT_VERSION = 1;

    public OutboxEventRecord(OutboxAggregateType aggregateType,
                             UUID aggregateId,
                             OutboxEventType eventType,
                             Map<String, Object> attributes) {

        this(
                aggregateType,
                aggregateId,
                eventType,
                null,
                aggregateId.toString(),
                DEFAULT_EVENT_VERSION,
                Map.of(),
                attributes
        );
    }

    public OutboxEventRecord {
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");

        messageKey = StringUtils.hasText(messageKey) ? messageKey : aggregateId.toString();
        eventVersion = eventVersion <= 0 ? DEFAULT_EVENT_VERSION : eventVersion;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        attributes = Map.copyOf(attributes);
    }
}
