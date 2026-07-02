package com.techora.payment.application.model;

public enum WebhookProcessResult {
    SUCCESS,
    ALREADY_HANDLED,
    AMOUNT_MISMATCH,
    PAYMENT_NOT_FOUND
}
