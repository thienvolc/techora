package com.techora.payment.application.result;

import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationResult(
        UUID attemptId,
        UUID paymentId,
        UUID orderId,
        UUID userId,
        String providerName,
        String providerReference,
        BigDecimal amount,
        PaymentAttemptStatus status,
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
    public static PaymentReconciliationResult from(PaymentAttempt attempt) {
        return new PaymentReconciliationResult(
                attempt.getId(),
                attempt.getPaymentId(),
                attempt.getOrderId(),
                attempt.getUserId(),
                attempt.getProviderName(),
                attempt.getProviderReference(),
                attempt.getAmount(),
                attempt.getStatus(),
                attempt.getProviderResponseCode(),
                attempt.getProviderStatusCode(),
                attempt.getProviderTransactionId(),
                attempt.getRawProviderPayload(),
                attempt.getProviderResultReceivedAt(),
                attempt.getReconciliationResolvedAt(),
                attempt.getReconciliationResolutionNote(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt()
        );
    }
}
