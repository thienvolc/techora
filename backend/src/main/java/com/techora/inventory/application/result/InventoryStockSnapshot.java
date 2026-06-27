package com.techora.inventory.application.result;

import java.time.Instant;
import java.util.UUID;

public record InventoryStockSnapshot(
        UUID productId,
        int quantityOnHand,
        int reservedQuantity,
        int availableQuantity,
        Instant updatedAt
) {
}
