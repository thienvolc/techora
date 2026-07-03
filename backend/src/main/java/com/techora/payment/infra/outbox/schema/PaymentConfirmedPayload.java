package com.techora.payment.infra.outbox.schema;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.valueobject.PaymentProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedPayload(
        int eventVersion,
        UUID paymentId,
        UUID attemptId,
        UUID orderId,
        UUID userId,
        PaymentProvider providerName,
        String providerReference,
        BigDecimal amount,
        Instant occurredAt
) implements PaymentEventPayload {
    public static PaymentConfirmedPayload fromEvent(PaymentConfirmedEvent event) {
        return new PaymentConfirmedPayload(
                event.eventVersion(),
                event.paymentId(),
                event.attemptId(),
                event.orderId(),
                event.userId(),
                event.providerName(),
                event.providerReference(),
                event.amount(),
                event.occurredAt()
        );
    }
}
