package com.techora.order.application.port.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReserveOrderInventoryCommand(
        UUID orderId,
        Instant expiresAt,
        List<ReserveOrderInventoryItemCommand> items
) {
    public ReserveOrderInventoryCommand {
        items = List.copyOf(items);
    }
}
