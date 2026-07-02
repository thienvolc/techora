package com.techora.orderhistory.application.view;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryView(
        UUID id,
        UUID orderId,
        String eventType,
        String oldStatus,
        String newStatus,
        String reason,
        Instant createdAt
) {
}
