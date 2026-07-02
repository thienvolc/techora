package com.techora.payment.infra.persistence;

import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.exception.PaymentAlreadyExistsException;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository jpaRepository;

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        try {
            return jpaRepository.save(PaymentAttemptJpaEntity.fromDomain(attempt))
                    .toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw new PaymentAlreadyExistsException();
        }
    }

    @Override
    public Optional<PaymentAttempt> findById(UUID attemptId) {
        return jpaRepository.findById(attemptId)
                .map(PaymentAttemptJpaEntity::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findLatestByPaymentId(UUID paymentId) {
        return jpaRepository.findFirstByPaymentIdOrderByCreatedAtDesc(paymentId)
        .map(PaymentAttemptJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<PaymentAttempt> findLockedReusablePendingByPaymentId(UUID paymentId, Instant now) {
    return jpaRepository.findLockedFirstByPaymentIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        paymentId,
                        PaymentAttemptStatus.PENDING,
                        now
                )
                .map(PaymentAttemptJpaEntity::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findByProviderReference(String providerReference) {
        return jpaRepository.findByProviderReference(providerReference)
                .map(PaymentAttemptJpaEntity::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findLockedById(UUID attemptId) {
        return jpaRepository.findLockedById(attemptId)
                .map(PaymentAttemptJpaEntity::toDomain);
    }

    @Override
    public List<PaymentAttempt> findExpiredPendingForUpdate(Instant now, int limit) {
        return jpaRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentAttemptStatus.PENDING,
                        now,
                        PageRequest.of(0, Math.max(1, limit))
                )
                .stream()
                .map(PaymentAttemptJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Page<PaymentAttempt> findUnresolvedReconciliationRequired(Pageable pageable) {
        return jpaRepository.findByStatusAndReconciliationResolvedAtIsNull(
                        PaymentAttemptStatus.RECONCILIATION_REQUIRED,
                        pageable
                )
                .map(PaymentAttemptJpaEntity::toDomain);
    }
}
