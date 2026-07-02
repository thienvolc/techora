package com.techora.order.application.port.payment;

public enum PendingPaymentExpirationResult {
    EXPIRED,
    ALREADY_PAID,
    NOT_PENDING,
    NOT_FOUND
}
