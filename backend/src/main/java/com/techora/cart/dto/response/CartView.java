package com.techora.cart.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartView(
        UUID id,
        UUID userId,
        List<CartItemView> items,
        BigDecimal total,
        Instant updatedAt
) {
    public CartView {
        items = List.copyOf(items);
    }
}
