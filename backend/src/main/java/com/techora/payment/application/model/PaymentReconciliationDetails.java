package com.techora.payment.application.model;

import com.techora.payment.domain.entity.PaymentAttempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationDetails(
        UUID attemptId,
        UUID paymentId,
        UUID orderId,
        UUID userId,
        String providerName,
        String providerReference,
        BigDecimal amount,
        String status,
        String providerResponseCode,
        String providerStatusCode,
        String providerTransactionId,
        String rawProviderPayload,
        Instant providerResultReceivedAt,
        Instant reconciliationResolvedAt,
        String reconciliationResolutionNote,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentReconciliationDetails from(PaymentAttempt attempt) {
        return new PaymentReconciliationDetails(
                attempt.getId(),
                attempt.getPaymentId(),
                attempt.getOrderId(),
                attempt.getUserId(),
                attempt.getProviderName().name(),
                attempt.getProviderReference(),
                attempt.getAmount(),
                attempt.getStatus().name(),
                attempt.getProviderResponseCode(),
                attempt.getProviderStatusCode(),
                attempt.getProviderTransactionId(),
                attempt.getProviderRawPayload(),
                attempt.getProviderResultReceivedAt(),
                attempt.getReconciliationResolvedAt(),
                attempt.getReconciliationNote(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt()
        );
    }
}
