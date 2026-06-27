package com.techora.order.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(
        UUID userId,
        String username,
        BigDecimal total,
        List<PlaceOrderItemCommand> items
) {
    public PlaceOrderCommand {
        items = List.copyOf(items);
    }
}
