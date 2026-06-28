package com.techora.payment.domain.valueobject;

public enum PaymentReconciliationReason {
    LATE_SUCCESS_AFTER_EXPIRED,
    LATE_SUCCESS_AFTER_CANCELLED,
    LATE_SUCCESS_AFTER_FAILED,
    SUCCESS_AFTER_PAYMENT_NOT_PAYABLE
}
