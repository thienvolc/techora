package com.techora.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemView(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        String slug,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
