package com.techora.payment.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.domain.valueobject.ProviderPaymentEvidence;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class PaymentAttempt extends AggregateRoot<UUID> {
    private final UUID paymentId;
    private final UUID orderId;
    private final UUID userId;
    private final String providerName;
    private final String providerReference;
    private final BigDecimal amount;
    private final Instant expiresAt;
    private final Instant createdAt;

    private PaymentAttemptStatus status;
    private String providerResponseCode;
    private String providerStatusCode;
    private String providerTransactionId;
    private String rawProviderPayload;
    private Instant providerResultReceivedAt;
    private Instant paidAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant reconciliationResolvedAt;
    private String reconciliationResolutionNote;
    private Instant updatedAt;

    @Builder
    private PaymentAttempt(UUID id,
                           UUID paymentId,
                           UUID orderId,
                           UUID userId,
                           String providerName,
                           String providerReference,
                           BigDecimal amount,
                           PaymentAttemptStatus status,
                           String providerResponseCode,
                           String providerStatusCode,
                           String providerTransactionId,
                           String rawProviderPayload,
                           Instant providerResultReceivedAt,
                           Instant expiresAt,
                           Instant paidAt,
                           Instant failedAt,
                           Instant expiredAt,
                           Instant reconciliationResolvedAt,
                           String reconciliationResolutionNote,
                           Instant createdAt,
                           Instant updatedAt) {

        super(id);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.providerName = providerName;
        this.providerReference = providerReference;
        this.amount = amount;
        this.status = status;
        this.providerResponseCode = providerResponseCode;
        this.providerStatusCode = providerStatusCode;
        this.providerTransactionId = providerTransactionId;
        this.rawProviderPayload = rawProviderPayload;
        this.providerResultReceivedAt = providerResultReceivedAt;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;
        this.failedAt = failedAt;
        this.expiredAt = expiredAt;
        this.reconciliationResolvedAt = reconciliationResolvedAt;
        this.reconciliationResolutionNote = reconciliationResolutionNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentAttempt createPending(UUID paymentId,
                                               UUID orderId,
                                               UUID userId,
                                               String providerName,
                                               String providerReference,
                                               BigDecimal amount,
                                               Instant expiresAt,
                                               Instant now) {

        return PaymentAttempt.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .providerName(providerName)
                .providerReference(providerReference)
                .amount(amount)
                .status(PaymentAttemptStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isPending() {
        return status == PaymentAttemptStatus.PENDING;
    }

    public boolean isExpired(Instant now) {
        return status == PaymentAttemptStatus.PENDING && !expiresAt.isAfter(now);
    }

    public boolean canAutoConfirm(Instant now) {
        return status == PaymentAttemptStatus.PENDING && expiresAt.isAfter(now);
    }

    public boolean isProviderSuccessAlreadyHandled() {
        return status == PaymentAttemptStatus.PAID
                || status == PaymentAttemptStatus.RECONCILIATION_REQUIRED;
    }

    public boolean markPaid(ProviderPaymentEvidence evidence, Instant now) {
        if (status == PaymentAttemptStatus.PAID) {
            return false;
        }
        recordProviderEvidence(evidence);
        changeStatus(PaymentAttemptStatus.PAID, now);
        paidAt = now;
        return true;
    }

    public boolean markFailed(ProviderPaymentEvidence evidence, Instant now) {
        if (status == PaymentAttemptStatus.FAILED) {
            return false;
        }
        recordProviderEvidence(evidence);
        changeStatus(PaymentAttemptStatus.FAILED, now);
        failedAt = now;
        return true;
    }

    public boolean markExpired(Instant now) {
        if (status == PaymentAttemptStatus.EXPIRED) {
            return false;
        }
        changeStatus(PaymentAttemptStatus.EXPIRED, now);
        expiredAt = now;
        return true;
    }

    public boolean markReconciliationRequired(ProviderPaymentEvidence evidence, Instant now) {
        if (status == PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            return false;
        }
        recordProviderEvidence(evidence);
        changeStatus(PaymentAttemptStatus.RECONCILIATION_REQUIRED, now);
        return true;
    }

    public void resolveReconciliation(String note, Instant now) {
        if (status != PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        reconciliationResolvedAt = now;
        reconciliationResolutionNote = note;
        updatedAt = now;
    }

    public boolean isReconciliationResolved() {
        return reconciliationResolvedAt != null;
    }

    private void recordProviderEvidence(ProviderPaymentEvidence evidence) {
        providerResponseCode = evidence.responseCode();
        providerStatusCode = evidence.providerStatusCode();
        providerTransactionId = evidence.providerTransactionId();
        rawProviderPayload = evidence.rawPayload();
        providerResultReceivedAt = evidence.receivedAt();
    }

    private void changeStatus(PaymentAttemptStatus nextStatus, Instant now) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        status = nextStatus;
        updatedAt = now;
    }
}
