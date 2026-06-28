package com.techora.payment.application.mapper;

import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResult toResult(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                null,
                payment.getPaymentWindowExpiresAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public PaymentResult toResult(Payment payment, PaymentAttempt attempt) {
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                statusFor(payment, attempt),
                attempt.getProviderReference(),
                attempt.getExpiresAt(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt()
        );
    }

    private PaymentStatus statusFor(Payment payment, PaymentAttempt attempt) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getStatus();
        }

        return switch (attempt.getStatus()) {
            case PENDING -> PaymentStatus.PENDING;
            case PAID -> PaymentStatus.PAID;
            case FAILED -> PaymentStatus.FAILED;
            case EXPIRED -> PaymentStatus.EXPIRED;
            case CANCELLED -> PaymentStatus.CANCELLED;
            case RECONCILIATION_REQUIRED -> PaymentStatus.RECONCILIATION_REQUIRED;
        };
    }
}
