package com.techora.inventory.application.command;

import java.util.UUID;

public record ReserveInventoryItem(
        UUID productId,
        int quantity
) {
}
