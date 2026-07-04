package com.techora.catalog.application.model;

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
        String status,
        CatalogCategorySnapshot category,
        Instant createdAt,
        Instant updatedAt
) {
    private static final String INACTIVE = "INACTIVE";

    public boolean isInactive() {
        return INACTIVE.equals(status) || !category.active();
    }
}
