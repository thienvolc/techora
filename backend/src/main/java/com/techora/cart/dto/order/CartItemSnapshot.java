package com.techora.cart.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemSnapshot(
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity
) {
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
