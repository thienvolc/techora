package com.techora.payment.domain.valueobject;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == PAID || nextStatus == FAILED;
            case PAID -> nextStatus == REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }
}
