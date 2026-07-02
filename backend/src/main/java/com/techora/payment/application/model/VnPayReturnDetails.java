package com.techora.payment.application.model;

import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VnPayReturnDetails(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant expiresAt,
        String message
) {
}
