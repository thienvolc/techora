package com.techora.order.application.result;

import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResult(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemResult> items,
        Instant createdAt,
        Instant updatedAt
) {
}
