package com.techora.catalog.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        int stockQuantity,
        String status,
        CategoryView category,
        Instant createdAt,
        Instant updatedAt
) {
}
