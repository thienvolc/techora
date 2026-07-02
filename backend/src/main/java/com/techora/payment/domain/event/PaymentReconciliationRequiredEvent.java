package com.techora.payment.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationRequiredEvent(
        int eventVersion,
        UUID paymentId,
        UUID attemptId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentProvider providerName,
        String providerReference,
        PaymentReconciliationReason reason,
        Instant occurredAt
) implements InternalEvent {
    private static final int CURRENT_VERSION = 1;

    @Override
    public UUID aggregateId() {
        return paymentId;
    }

    public PaymentReconciliationRequiredEvent(PaymentAttempt attempt,
                                              PaymentReconciliationReason reason) {

        this(
                CURRENT_VERSION,
                attempt.getPaymentId(),
                attempt.getId(),
                attempt.getOrderId(),
                attempt.getUserId(),
                attempt.getAmount(),
                attempt.getProviderName(),
                attempt.getProviderReference(),
                reason,
                attempt.getUpdatedAt()
        );
    }
}
