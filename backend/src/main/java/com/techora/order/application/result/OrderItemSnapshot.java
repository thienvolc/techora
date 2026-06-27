package com.techora.order.application.result;

import com.techora.order.domain.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSnapshot(
        UUID itemId,
        UUID productId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static OrderItemSnapshot from(OrderItem item) {
        return new OrderItemSnapshot(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
