package com.techora.inventory.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryProductStockView(
        UUID id,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        int stockQuantity,
        String status,
        InventoryCategoryView category,
        Instant createdAt,
        Instant updatedAt
) {
}
