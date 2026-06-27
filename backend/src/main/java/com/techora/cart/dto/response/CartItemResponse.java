package com.techora.cart.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CartItemResponse(
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
