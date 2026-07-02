package com.techora.order.application.model;

import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaceOrderView(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal total,
        UUID paymentId,
        String paymentUrl,
        Instant paymentExpiresAt,
        List<PlaceOrderItemView> items,
        Instant createdAt,
        Instant updatedAt
) {
}
