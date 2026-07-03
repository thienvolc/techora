package com.techora.outbox.dto;

import com.techora.outbox.entity.OutboxEventEntity;

import java.util.Map;
import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        String topic,
        String messageKey,
        Map<String, String> headers,
        String payload
) {
}
