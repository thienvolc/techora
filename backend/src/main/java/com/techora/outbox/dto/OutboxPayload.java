package com.techora.outbox.dto;

import java.util.Map;
import java.util.UUID;

public record OutboxPayload(
        UUID eventId,
        int eventVersion,
        Map<String, Object> attributes
) {

}
