package com.techora.payment.application.result;

import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResult(
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
}
