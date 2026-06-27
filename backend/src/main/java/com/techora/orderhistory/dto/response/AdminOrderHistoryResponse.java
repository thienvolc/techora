package com.techora.orderhistory.dto.response;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.orderhistory.entity.OrderHistoryActorType;
import com.techora.orderhistory.entity.OrderHistoryEventType;

import java.time.Instant;
import java.util.UUID;

public record AdminOrderHistoryResponse(
        UUID id,
        UUID orderId,
        OrderHistoryEventType eventType,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        String reason,
        String metadata,
        OrderHistoryActorType actorType,
        UUID actorId,
        String actorName,
        Instant createdAt
) {
}
