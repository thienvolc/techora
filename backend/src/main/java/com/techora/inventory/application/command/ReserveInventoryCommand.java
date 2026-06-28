package com.techora.inventory.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReserveInventoryCommand(
        UUID orderId,
        Instant expiresAt,
        List<ReserveInventoryItem> items
) {
    public ReserveInventoryCommand {
        items = List.copyOf(items);
    }
}
