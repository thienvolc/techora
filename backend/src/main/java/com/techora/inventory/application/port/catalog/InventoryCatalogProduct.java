package com.techora.inventory.application.port.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryCatalogProduct(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        String status,
        InventoryCatalogCategory category,
        Instant createdAt,
        Instant updatedAt
) {
}
