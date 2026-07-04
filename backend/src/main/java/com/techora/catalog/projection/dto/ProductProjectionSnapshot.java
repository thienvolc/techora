package com.techora.catalog.projection.dto;

import com.techora.catalog.domain.valueobject.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductProjectionSnapshot(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status,
        CategoryProjectionSnapshot category,
        Instant createdAt,
        Instant updatedAt
) {
}
