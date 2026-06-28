package com.techora.payment.application.result;

import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VnPayReturnResult(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant expiresAt,
        String message
) {
    public static VnPayReturnResult from(PaymentResult payment) {
        return new VnPayReturnResult(
                payment.id(),
                payment.orderId(),
                payment.amount(),
                payment.status(),
                payment.expiresAt(),
                messageFor(payment.status())
        );
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
        };
    }
}
