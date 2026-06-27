package com.techora.cart.dto.checkout;

import java.math.BigDecimal;
import java.util.UUID;

public record CartCheckoutItem(
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
