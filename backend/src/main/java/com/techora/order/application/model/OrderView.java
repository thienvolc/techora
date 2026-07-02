package com.techora.order.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
        UUID id,
        UUID userId,
        String status,
        BigDecimal total,
        List<OrderItemView> items,
        Instant paymentDeadlineAt,
        Instant createdAt,
        Instant updatedAt
) {
}
