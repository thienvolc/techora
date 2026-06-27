package com.techora.payment.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.domain.valueobject.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class Payment extends AggregateRoot<UUID> {

    private final UUID orderId;
    private final UUID userId;
    private final String username;
    private final BigDecimal amount;
    private final String providerReference;
    private final Instant createdAt;

    private PaymentStatus status;
    private Instant updatedAt;

    @Builder
    private Payment(UUID id,
                    UUID orderId,
                    UUID userId,
                    String username,
                    BigDecimal amount,
                    PaymentStatus status,
                    String providerReference,
                    Instant createdAt,
                    Instant updatedAt) {

        super(id);
        this.orderId = orderId;
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.status = status;
        this.providerReference = providerReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createPending(UUID orderId,
                                        UUID userId,
                                        String username,
                                        BigDecimal amount,
                                        String providerReference,
                                        Instant now) {

        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .username(username)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .providerReference(providerReference)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean markPaid(Instant now) {
        if (status == PaymentStatus.PAID) {
            return false;
        }
        changeStatus(PaymentStatus.PAID, now);
        return true;
    }

    public boolean markFailed(Instant now) {
        if (status == PaymentStatus.FAILED) {
            return false;
        }
        changeStatus(PaymentStatus.FAILED, now);
        return true;
    }

    private void changeStatus(PaymentStatus nextStatus, Instant now) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
//            throw new InvalidPaymentStatusTransitionException(status, nextStatus);
        }
        status = nextStatus;
        updatedAt = now;
    }
}
