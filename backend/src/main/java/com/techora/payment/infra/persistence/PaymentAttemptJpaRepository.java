package com.techora.payment.infra.persistence;

import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptJpaEntity, UUID> {

    Optional<PaymentAttemptJpaEntity> findFirstByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttemptJpaEntity> findFirstByPaymentIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID paymentId,
            PaymentAttemptStatus status,
            Instant expiresAt
    );

    Optional<PaymentAttemptJpaEntity> findByProviderReference(String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt from PaymentAttemptJpaEntity attempt
            where attempt.providerReference = :providerReference
            """)
    Optional<PaymentAttemptJpaEntity> findLockedByProviderReference(
            @Param("providerReference") String providerReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PaymentAttemptJpaEntity> findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentAttemptStatus status,
            Instant expiresAt,
            Pageable pageable
    );

    Page<PaymentAttemptJpaEntity> findByStatusAndReconciliationResolvedAtIsNull(
            PaymentAttemptStatus status,
            Pageable pageable
    );
}
