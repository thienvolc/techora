package com.techora.order.application.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record OrderPrice(
        BigDecimal total,
        Map<UUID, BigDecimal> subtotalByProductId
) {
    public OrderPrice {
        subtotalByProductId = Map.copyOf(subtotalByProductId);
    }

    public BigDecimal subtotalOf(UUID productId) {
        return subtotalByProductId.get(productId);
    }
}
