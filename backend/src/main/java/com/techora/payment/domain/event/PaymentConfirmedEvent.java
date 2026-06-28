package com.techora.payment.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedEvent(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String providerName,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return paymentId;
    }

    public PaymentConfirmedEvent(Payment payment) {
        this(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                null,
                payment.getUpdatedAt()
        );
    }

    public PaymentConfirmedEvent(Payment payment, String providerName) {
        this(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                providerName,
                payment.getUpdatedAt()
        );
    }
}
