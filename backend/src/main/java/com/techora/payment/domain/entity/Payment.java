package com.techora.payment.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
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
    private final Instant orderPaymentDeadlineAt;
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
                    Instant orderPaymentDeadlineAt,
                    Instant createdAt,
                    Instant updatedAt) {

        super(id);
        this.orderId = orderId;
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.status = status;
        this.orderPaymentDeadlineAt = orderPaymentDeadlineAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createPending(UUID orderId,
                                        UUID userId,
                                        String username,
                                        BigDecimal amount,
                                        Instant orderPaymentDeadlineAt,
                                        Instant now) {

        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .username(username)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .orderPaymentDeadlineAt(orderPaymentDeadlineAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public PaymentReconciliationReason reconciliationReasonForSuccessfulProviderResult(Instant now) {
        if (status == PaymentStatus.PENDING && !orderPaymentDeadlineAt.isAfter(now)) {
            return PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
        }
        return switch (status) {
            case EXPIRED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
            case CANCELLED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_CANCELLED;
            case FAILED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_FAILED;
            default -> PaymentReconciliationReason.SUCCESS_AFTER_PAYMENT_NOT_PAYABLE;
        };
    }

    public void markPaid(Instant now) {
        if (isPaid()) {
            return;
        }
        changeStatus(PaymentStatus.PAID, now);
    }

    public void markExpired(Instant now) {
        if (isExpired(now)) {
            return;
        }
        changeStatus(PaymentStatus.EXPIRED, now);
    }

    public void markReconciliationRequired(Instant now) {
        if (isReconciliationRequired()) {
            return;
        }
        changeStatus(PaymentStatus.RECONCILIATION_REQUIRED, now);
    }

    private void changeStatus(PaymentStatus nextStatus, Instant now) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
//            throw new InvalidPaymentStatusTransitionException(status, nextStatus);
        }
        status = nextStatus;
        updatedAt = now;
    }

    public boolean canCreateAttempt() {
        return isPending();
    }

    private boolean isExpired(Instant now) {
        return status == PaymentStatus.EXPIRED || isPendingExpired(now);
    }

    private boolean isPendingExpired(Instant now) {
        return isPending() && isPastDue(now);
    }

    private boolean isPastDue(Instant now) {
        return !orderPaymentDeadlineAt.isAfter(now);
    }

    public boolean isReconciliationRequired() {
        return status == PaymentStatus.RECONCILIATION_REQUIRED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }
}
