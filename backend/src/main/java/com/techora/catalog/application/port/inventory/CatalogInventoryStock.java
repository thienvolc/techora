package com.techora.catalog.application.port.inventory;

import java.time.Instant;
import java.util.UUID;

public record CatalogInventoryStock(
        UUID productId,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        Instant updatedAt
) {
}
