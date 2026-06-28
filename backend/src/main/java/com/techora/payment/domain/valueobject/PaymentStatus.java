package com.techora.payment.domain.valueobject;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    EXPIRED,
    CANCELLED,
    RECONCILIATION_REQUIRED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == PAID
                    || nextStatus == FAILED
                    || nextStatus == EXPIRED
                    || nextStatus == CANCELLED
                    || nextStatus == RECONCILIATION_REQUIRED;
            case PAID -> nextStatus == REFUNDED;
            case FAILED, EXPIRED, CANCELLED -> nextStatus == RECONCILIATION_REQUIRED;
            case RECONCILIATION_REQUIRED, REFUNDED -> false;
        };
    }
}
