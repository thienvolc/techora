package com.techora.payment.domain.valueobject;

import java.time.Instant;

public record ProviderPaymentEvidence(
        String responseCode,
        String providerStatusCode,
        String providerTransactionId,
        String rawPayload,
        Instant receivedAt
) {
}
