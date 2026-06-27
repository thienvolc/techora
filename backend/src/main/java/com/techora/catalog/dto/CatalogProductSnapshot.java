package com.techora.catalog.dto;

import com.techora.catalog.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogProductSnapshot(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        ProductStatus status,
        CatalogCategorySnapshot category,
        Instant createdAt,
        Instant updatedAt
) {
}
