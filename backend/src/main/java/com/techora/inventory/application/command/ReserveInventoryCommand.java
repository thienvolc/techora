package com.techora.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryCommand(
        UUID orderId,
        List<ReserveInventoryItem> items
) {
    public ReserveInventoryCommand {
        items = List.copyOf(items);
    }
}
