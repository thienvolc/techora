package com.techora.order.domain.entity;

public enum OrderStatus {
    CREATED,
    STOCK_RESERVED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    FULFILLING,
    SHIPPED,
    DELIVERED;

    public static boolean isCancelled(OrderStatus newStatus) {
        return newStatus == CANCELLED;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }
}
