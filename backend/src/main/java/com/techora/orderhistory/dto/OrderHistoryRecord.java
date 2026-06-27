package com.techora.orderhistory.dto;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.orderhistory.entity.OrderReasons;
import com.techora.orderhistory.entity.OrderHistoryActorType;
import com.techora.orderhistory.entity.OrderHistoryEventType;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.UUID;

@Builder
public record OrderHistoryRecord(
        UUID orderId,
        UUID ownerUserId,

        OrderHistoryEventType eventType,

        @Nullable
        OrderStatus oldStatus,
        OrderStatus newStatus,
        OrderReasons reason,

        String metadata,

        OrderHistoryActorType actorType,
        UUID actorId,
        String actorName
) {
}
