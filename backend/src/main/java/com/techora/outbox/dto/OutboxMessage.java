package com.techora.outbox.dto;

import com.techora.outbox.constant.OutboxEventType;

import java.util.Map;
import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        String topic,
        String messageKey,
        OutboxEventType eventType,
        int eventVersion,
        Map<String, String> headers,
        String payload
) {
    public OutboxMessage {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
