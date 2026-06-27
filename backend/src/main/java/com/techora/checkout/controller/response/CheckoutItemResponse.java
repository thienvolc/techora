package com.techora.checkout.controller.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
