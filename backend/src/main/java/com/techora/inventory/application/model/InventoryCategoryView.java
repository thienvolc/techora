package com.techora.inventory.application.model;

import com.techora.inventory.application.port.catalog.InventoryCatalogCategory;

import java.time.Instant;
import java.util.UUID;

public record InventoryCategoryView(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryCategoryView from(InventoryCatalogCategory category) {
        return new InventoryCategoryView(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.active(),
                category.createdAt(),
                category.updatedAt()
        );
    }
}
