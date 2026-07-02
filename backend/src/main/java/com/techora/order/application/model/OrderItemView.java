package com.techora.order.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemView(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
