package com.techora.outbox.dto;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;

import java.util.Map;
import java.util.UUID;

public record OutboxEventRecord(
        OutboxAggregateType aggregateType,
        UUID aggregateId,
        OutboxEventType eventType,
        Map<String, Object> attributes
) {
    public OutboxEventRecord {
        attributes = Map.copyOf(attributes);
    }
}
