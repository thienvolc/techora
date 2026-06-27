package com.techora.cart.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CartResponse(
        UUID id,
        UUID userId,
        List<CartItemResponse> items,
        BigDecimal total,
        Instant updatedAt
) {
}
