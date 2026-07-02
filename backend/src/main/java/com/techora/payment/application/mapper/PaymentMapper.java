package com.techora.payment.application.mapper;

import com.techora.payment.application.model.PaymentDetails;
import com.techora.payment.application.model.VnPayReturnDetails;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentDetails toDetails(Payment payment) {
        return new PaymentDetails(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                null,
                payment.getOrderPaymentDeadlineAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public VnPayReturnDetails toReturnDetails(Payment payment, PaymentAttempt attempt) {
        PaymentStatus status = statusFor(payment, attempt);
        String message = messageFor(status);

        return new VnPayReturnDetails(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                status,
                attempt.getExpiresAt(),
                message
        );
    }

    public PaymentDetails toDetails(Payment payment, PaymentAttempt attempt) {
        return new PaymentDetails(
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

    private static String messageFor(PaymentStatus status) {
        return switch (status) {
            case PAID -> "Payment confirmed";
            case FAILED -> "Payment failed";
            case EXPIRED -> "Payment expired";
            case CANCELLED -> "Payment cancelled";
            case PENDING -> "Payment is waiting for VNPAY confirmation";
            case RECONCILIATION_REQUIRED -> "Payment received and requires manual verification";
            case REFUNDED -> "Payment refunded";
            default -> "Payment status is unknown";
        };
    }
}
