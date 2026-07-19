package com.techora.payment.infra.persistence;

import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts", indexes = {
        @Index(name = "idx_payment_attempts_payment_created_at", columnList = "payment_id, created_at"),
        @Index(name = "idx_payment_attempts_status_expires_at", columnList = "status, expires_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_attempts_provider_reference", columnNames = "provider_reference")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_name", nullable = false, length = 40)
    private String providerName;

    @Column(name = "provider_reference", nullable = false, length = 80)
    private String providerReference;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentAttemptStatus status;

    @Column(name = "provider_response_code", length = 40)
    private String providerResponseCode;

    @Column(name = "provider_status_code", length = 40)
    private String providerStatusCode;

    @Column(name = "provider_transaction_id", length = 80)
    private String providerTransactionId;

    @Column(name = "raw_provider_payload", columnDefinition = "TEXT")
    private String rawProviderPayload;

    @Column(name = "provider_result_received_at")
    private Instant providerResultReceivedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "reconciliation_resolved_at")
    private Instant reconciliationResolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_reason", length = 60)
    private PaymentReconciliationReason reconciliationReason;

    @Column(name = "reconciliation_resolution_note", columnDefinition = "TEXT")
    private String reconciliationResolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PaymentAttemptJpaEntity fromDomain(PaymentAttempt attempt) {
        return PaymentAttemptJpaEntity.builder()
                .id(attempt.getId())
                .paymentId(attempt.getPaymentId())
                .orderId(attempt.getOrderId())
                .userId(attempt.getUserId())
                .providerName(attempt.getProviderName().name())
                .providerReference(attempt.getProviderReference())
                .amount(attempt.getAmount())
                .status(attempt.getStatus())
                .providerResponseCode(attempt.getProviderResponseCode())
                .providerStatusCode(attempt.getProviderStatusCode())
                .providerTransactionId(attempt.getProviderTransactionId())
                .rawProviderPayload(attempt.getProviderRawPayload())
                .providerResultReceivedAt(attempt.getProviderResultReceivedAt())
                .expiresAt(attempt.getExpiresAt())
                .paidAt(attempt.getPaidAt())
                .failedAt(attempt.getFailedAt())
                .expiredAt(attempt.getExpiredAt())
                .reconciliationResolvedAt(attempt.getReconciliationResolvedAt())
                .reconciliationReason(attempt.getReconciliationReason())
                .reconciliationResolutionNote(attempt.getReconciliationNote())
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .build();
    }

    public PaymentAttempt toDomain() {
        return PaymentAttempt.builder()
                .id(id)
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .providerName(PaymentProvider.valueOf(providerName))
                .providerReference(providerReference)
                .amount(amount)
                .status(status)
                .providerResponseCode(providerResponseCode)
                .providerStatusCode(providerStatusCode)
                .providerTransactionId(providerTransactionId)
                .providerRawPayload(rawProviderPayload)
                .providerResultReceivedAt(providerResultReceivedAt)
                .expiresAt(expiresAt)
                .paidAt(paidAt)
                .failedAt(failedAt)
                .expiredAt(expiredAt)
                .reconciliationResolvedAt(reconciliationResolvedAt)
                .reconciliationReason(reconciliationReason)
                .reconciliationNote(reconciliationResolutionNote)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
