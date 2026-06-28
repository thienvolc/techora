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
    private final Instant paymentWindowExpiresAt;
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
                    Instant paymentWindowExpiresAt,
                    Instant createdAt,
                    Instant updatedAt) {

        super(id);
        this.orderId = orderId;
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.status = status;
        this.paymentWindowExpiresAt = paymentWindowExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment createPending(UUID orderId,
                                        UUID userId,
                                        String username,
                                        BigDecimal amount,
                                        Instant paymentWindowExpiresAt,
                                        Instant now) {

        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .username(username)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .paymentWindowExpiresAt(paymentWindowExpiresAt)
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

    public boolean markExpired(Instant now) {
        if (status == PaymentStatus.EXPIRED) {
            return false;
        }
        changeStatus(PaymentStatus.EXPIRED, now);
        return true;
    }

    public boolean markCancelled(Instant now) {
        if (status == PaymentStatus.CANCELLED) {
            return false;
        }
        changeStatus(PaymentStatus.CANCELLED, now);
        return true;
    }

    public boolean markReconciliationRequired(Instant now) {
        if (status == PaymentStatus.RECONCILIATION_REQUIRED) {
            return false;
        }
        changeStatus(PaymentStatus.RECONCILIATION_REQUIRED, now);
        return true;
    }

    public boolean isExpired(Instant now) {
        return status == PaymentStatus.PENDING && !paymentWindowExpiresAt.isAfter(now);
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    public boolean isProviderSuccessAlreadyHandled() {
        return status == PaymentStatus.PAID
                || status == PaymentStatus.RECONCILIATION_REQUIRED
                || status == PaymentStatus.REFUNDED;
    }

    public boolean canAutoConfirm(Instant now) {
        return status == PaymentStatus.PENDING && paymentWindowExpiresAt.isAfter(now);
    }

    public boolean canFailFromProvider() {
        return status == PaymentStatus.PENDING;
    }

    public PaymentReconciliationReason reconciliationReasonForSuccessfulProviderResult(Instant now) {
        if (status == PaymentStatus.PENDING && !paymentWindowExpiresAt.isAfter(now)) {
            return PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
        }
        return switch (status) {
            case EXPIRED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
            case CANCELLED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_CANCELLED;
            case FAILED -> PaymentReconciliationReason.LATE_SUCCESS_AFTER_FAILED;
            default -> PaymentReconciliationReason.SUCCESS_AFTER_PAYMENT_NOT_PAYABLE;
        };
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
