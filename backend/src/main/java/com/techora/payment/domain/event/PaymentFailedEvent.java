package com.techora.payment.domain.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        Instant occurredAt
) implements InternalEvent {

    @Override
    public UUID aggregateId() {
        return paymentId;
    }

    public PaymentFailedEvent(Payment payment) {
        this(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getUpdatedAt()
        );
    }
}
