package com.techora.checkout.controller.response;

import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal total,
        UUID paymentId,
        String paymentUrl,
        Instant paymentExpiresAt,
        List<CheckoutItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
