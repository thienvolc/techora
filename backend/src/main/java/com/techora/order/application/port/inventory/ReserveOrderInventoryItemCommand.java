package com.techora.order.application.port.inventory;

import java.util.UUID;

public record ReserveOrderInventoryItemCommand(
        UUID productId,
        int quantity
) {
}
