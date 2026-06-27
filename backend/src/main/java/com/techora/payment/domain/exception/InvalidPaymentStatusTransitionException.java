package com.techora.payment.domain.exception;

import com.techora.payment.domain.valueobject.PaymentStatus;

public class InvalidPaymentStatusTransitionException extends RuntimeException {

    public InvalidPaymentStatusTransitionException(PaymentStatus currentStatus, PaymentStatus nextStatus) {
        super("Invalid payment status transition: " + currentStatus + " -> " + nextStatus);
    }
}
