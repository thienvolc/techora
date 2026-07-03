package com.techora.payment.infra.outbox.schema;

import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.valueobject.PaymentProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedPayload(
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
    public static PaymentFailedPayload fromEvent(PaymentFailedEvent event) {
        return new PaymentFailedPayload(
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
