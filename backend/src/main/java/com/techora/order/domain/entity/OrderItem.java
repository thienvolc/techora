package com.techora.order.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderItem {
    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String sku;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal subtotal;
}
