package com.techora.inventory.application.port.catalog;

import java.time.Instant;
import java.util.UUID;

public record InventoryCatalogCategory(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
