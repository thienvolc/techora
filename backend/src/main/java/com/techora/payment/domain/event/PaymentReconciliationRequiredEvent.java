package com.techora.payment.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationRequiredEvent(
        UUID paymentId,
        UUID attemptId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String providerName,
        String providerReference,
        PaymentReconciliationReason reason,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return paymentId;
    }

    public PaymentReconciliationRequiredEvent(Payment payment,
                                              PaymentAttempt attempt,
                                              String providerName,
                                              PaymentReconciliationReason reason) {

        this(
                payment.getId(),
                attempt.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                providerName,
                attempt.getProviderReference(),
                reason,
                attempt.getUpdatedAt()
        );
    }
}
