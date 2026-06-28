package com.techora.order.application.port.payment;

public enum PaymentWindowExpirationResult {
    EXPIRED,
    ALREADY_PAID,
    NOT_EXPIRABLE,
    NOT_FOUND
}
