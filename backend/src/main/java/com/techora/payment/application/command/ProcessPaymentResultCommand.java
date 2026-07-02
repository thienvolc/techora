package com.techora.payment.application.command;

import java.math.BigDecimal;
import java.time.Instant;

public record ProcessPaymentResultCommand(
        String providerReference,
        BigDecimal amount,
        boolean successful,
        String responseCode,
        String providerStatusCode,
        String providerTransactionId,
        String rawPayload,
        Instant receivedAt
) {
}
