package com.techora.orderhistory.dto.response;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.orderhistory.entity.OrderHistoryEventType;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID id,
        UUID orderId,
        OrderHistoryEventType eventType,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String reason,
        Instant createdAt
) {
}
