package com.techora.order.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderItemCommand(
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
