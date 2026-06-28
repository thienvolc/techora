package com.techora.payment.controller.response;

import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String providerReference,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                result.id(),
                result.orderId(),
                result.userId(),
                result.amount(),
                result.status(),
                result.providerReference(),
                result.expiresAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
