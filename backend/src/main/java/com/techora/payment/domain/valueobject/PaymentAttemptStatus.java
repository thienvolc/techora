package com.techora.payment.domain.valueobject;

public enum PaymentAttemptStatus {
    PENDING,
    PAID,
    FAILED,
    EXPIRED,
    CANCELLED,
    RECONCILIATION_REQUIRED;

    public boolean canTransitionTo(PaymentAttemptStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == PAID
                    || nextStatus == FAILED
                    || nextStatus == EXPIRED
                    || nextStatus == CANCELLED
                    || nextStatus == RECONCILIATION_REQUIRED;
            case FAILED, EXPIRED, CANCELLED -> nextStatus == RECONCILIATION_REQUIRED;
            case PAID, RECONCILIATION_REQUIRED -> false;
        };
    }
}
