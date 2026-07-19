package com.techora.payment.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
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
    private final PaymentProvider providerName;
    private final String providerReference;
    private final BigDecimal amount;
    private final Instant expiresAt;
    private final Instant createdAt;

    private PaymentAttemptStatus status;
    private String providerResponseCode;
    private String providerStatusCode;
    private String providerTransactionId;
    private String providerRawPayload;
    private Instant providerResultReceivedAt;

    private Instant paidAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant reconciliationResolvedAt;
    private PaymentReconciliationReason reconciliationReason;
    private String reconciliationNote;

    private Instant updatedAt;

    @Builder
    private PaymentAttempt(UUID id,
                           UUID paymentId,
                           UUID orderId,
                           UUID userId,
                           PaymentProvider providerName,
                           String providerReference,
                           BigDecimal amount,
                           PaymentAttemptStatus status,
                           String providerResponseCode,
                           String providerStatusCode,
                           String providerTransactionId,
                           String providerRawPayload,
                           Instant providerResultReceivedAt,
                           Instant expiresAt,
                           Instant paidAt,
                           Instant failedAt,
                           Instant expiredAt,
                           Instant reconciliationResolvedAt,
                           PaymentReconciliationReason reconciliationReason,
                           String reconciliationNote,
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
        this.providerRawPayload = providerRawPayload;
        this.providerResultReceivedAt = providerResultReceivedAt;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;
        this.failedAt = failedAt;
        this.expiredAt = expiredAt;
        this.reconciliationResolvedAt = reconciliationResolvedAt;
        this.reconciliationReason = reconciliationReason;
        this.reconciliationNote = reconciliationNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentAttempt createPending(UUID paymentId,
                                               UUID orderId,
                                               UUID userId,
                                               PaymentProvider providerName,
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

    public void markPaid(ProviderPaymentEvidence evidence) {
        if (isPaid()) {
            return;
        }
        updateStatus(PaymentAttemptStatus.PAID, evidence.receivedAt());
        recordProviderEvidence(evidence);
        paidAt = evidence.receivedAt();
    }

    public void markFailed(ProviderPaymentEvidence evidence) {
        if (isFailed()) {
            return;
        }
        updateStatus(PaymentAttemptStatus.FAILED, evidence.receivedAt());
        recordProviderEvidence(evidence);
        failedAt = evidence.receivedAt();
    }

    public void markExpired(Instant now) {
        if (!isPendingPastDue(now)) {
            return;
        }
        updateStatus(PaymentAttemptStatus.EXPIRED, now);
        expiredAt = now;
    }

    public void markReconciliationRequired(ProviderPaymentEvidence evidence,
                                           PaymentReconciliationReason reason) {
        if (isReconciliationRequired()) {
            return;
        }
        updateStatus(PaymentAttemptStatus.RECONCILIATION_REQUIRED, evidence.receivedAt());
        recordProviderEvidence(evidence);
        reconciliationReason = reason;
    }

    public void resolveReconciliation(String note, Instant now) {
        if (!isReconciliationRequired()) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        reconciliationResolvedAt = now;
        reconciliationNote = note;
        updatedAt = now;
    }

    private void updateStatus(PaymentAttemptStatus nextStatus, Instant now) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
        status = nextStatus;
        updatedAt = now;
    }

    private void recordProviderEvidence(ProviderPaymentEvidence evidence) {
        providerResponseCode = evidence.responseCode();
        providerStatusCode = evidence.providerStatusCode();
        providerTransactionId = evidence.providerTransactionId();
        providerRawPayload = evidence.rawPayload();
        providerResultReceivedAt = evidence.receivedAt();
    }

    public boolean isPendingPastDue(Instant now) {
        return isPending() && isPastDue(now);
    }

    public boolean isExpiredStatus() {
        return status == PaymentAttemptStatus.EXPIRED;
    }

    public boolean canAutoConfirm(Instant now) {
        return isPending() && !isPastDue(now);
    }

    public boolean hasHandledProviderResult() {
        return providerResultReceivedAt != null;
    }

    public boolean canApplyProviderFailure() {
        return isPending();
    }

    private boolean isPending() {
        return status == PaymentAttemptStatus.PENDING;
    }

    private boolean isPastDue(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isPaid() {
        return status == PaymentAttemptStatus.PAID;
    }

    private boolean isFailed() {
        return status == PaymentAttemptStatus.FAILED;
    }

    public boolean isReconciliationRequired() {
        return status == PaymentAttemptStatus.RECONCILIATION_REQUIRED;
    }
}
