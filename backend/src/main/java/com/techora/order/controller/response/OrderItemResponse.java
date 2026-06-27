package com.techora.order.controller.response;

import com.techora.order.application.result.OrderItemResult;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItemResult result) {
        return new OrderItemResponse(
                result.id(),
                result.productId(),
                result.productName(),
                result.sku(),
                result.unitPrice(),
                result.quantity(),
                result.subtotal()
        );
    }
}
