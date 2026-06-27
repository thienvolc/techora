package com.techora.orderhistory.entity;

import com.techora.order.domain.entity.OrderStatus;

public enum OrderHistoryEventType {
    ORDER_PLACED,
    ORDER_STATUS_CHANGED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    ORDER_CANCELLED;

    public static OrderHistoryEventType of(OrderStatus status) {
        return switch (status) {
            case CANCELLED -> ORDER_CANCELLED;
            case PAID -> PAYMENT_CONFIRMED;
            case PAYMENT_FAILED -> PAYMENT_FAILED;
            default -> ORDER_STATUS_CHANGED;
        };
    }
}
