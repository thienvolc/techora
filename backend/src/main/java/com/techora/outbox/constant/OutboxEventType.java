package com.techora.outbox.constant;

public enum OutboxEventType {
    ORDER_PLACED,
    ORDER_STATUS_CHANGED,
    STOCK_REDUCED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_RECONCILIATION_REQUIRED,
    ORDER_CANCELLED
}
