package com.techora.payment.infra.outbox.schema;

import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReconciliationRequiredPayload(
        int eventVersion,
        UUID paymentId,
        UUID attemptId,
        UUID orderId,
        UUID userId,
        PaymentProvider providerName,
        String providerReference,
        BigDecimal amount,
        Instant occurredAt,
        PaymentReconciliationReason reason

) implements PaymentEventPayload {
    public static PaymentReconciliationRequiredPayload fromEvent(PaymentReconciliationRequiredEvent event) {
        return new PaymentReconciliationRequiredPayload(
                event.eventVersion(),
                event.paymentId(),
                event.attemptId(),
                event.orderId(),
                event.userId(),
                event.providerName(),
                event.providerReference(),
                event.amount(),
                event.occurredAt(),
                event.reason()
        );
    }
}
