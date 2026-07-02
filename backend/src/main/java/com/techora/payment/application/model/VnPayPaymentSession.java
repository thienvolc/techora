package com.techora.payment.application.model;

import java.time.Instant;
import java.util.UUID;

public record VnPayPaymentSession(
        UUID paymentId,
        String paymentUrl,
        Instant expiresAt
) {
}
