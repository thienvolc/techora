package com.techora.inventory.application.result;

import com.techora.catalog.dto.CatalogCategorySnapshot;
import com.techora.catalog.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemProductResult(
        UUID productId,
        String name,
        String sku,
        String slug,
        String description,
        BigDecimal price,
        int quantityOnHand,
        ProductStatus status,
        CatalogCategorySnapshot category,
        Instant createdAt,
        Instant updatedAt
) {
}
