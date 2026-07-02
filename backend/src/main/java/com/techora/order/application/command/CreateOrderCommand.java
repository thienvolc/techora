package com.techora.order.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID userId,
        String username,
        BigDecimal total,
        List<CreateOrderItemCommand> items
) {
    public CreateOrderCommand {
        items = List.copyOf(items);
    }
}
