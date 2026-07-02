package com.techora.orderhistory.application.view;

import java.time.Instant;
import java.util.UUID;

public record AdminOrderHistoryView(
        UUID id,
        UUID orderId,
        String eventType,
        String oldStatus,
        String newStatus,
        String reason,
        String metadata,
        String actorType,
        UUID actorId,
        String actorName,
        Instant createdAt
) {
}
