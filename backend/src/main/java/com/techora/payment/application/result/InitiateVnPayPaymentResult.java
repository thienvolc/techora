package com.techora.payment.application.result;

import java.time.Instant;
import java.util.UUID;

public record InitiateVnPayPaymentResult(
        UUID paymentId,
        String paymentUrl,
        Instant expiresAt
) {
}
