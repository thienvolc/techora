package com.techora.payment.controller.response;

import com.techora.payment.application.result.PaymentReconciliationResult;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationResponse(
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
    public static PaymentReconciliationResponse from(PaymentReconciliationResult result) {
        return new PaymentReconciliationResponse(
                result.attemptId(),
                result.paymentId(),
                result.orderId(),
                result.userId(),
                result.providerName(),
                result.providerReference(),
                result.amount(),
                result.status(),
                result.providerResponseCode(),
                result.providerStatusCode(),
                result.providerTransactionId(),
                result.rawProviderPayload(),
                result.providerResultReceivedAt(),
                result.reconciliationResolvedAt(),
                result.reconciliationResolutionNote(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
