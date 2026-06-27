package com.techora.order.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResult(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
