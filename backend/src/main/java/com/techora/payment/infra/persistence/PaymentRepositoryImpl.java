package com.techora.payment.infra.persistence;

import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.exception.PaymentAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        try {
            return jpaRepository.save(PaymentJpaEntity.fromDomain(payment))
                    .toDomain();
        } catch (DataIntegrityViolationException ex) {
            throw new PaymentAlreadyExistsException();
        }
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByIdAndUserId(UUID paymentId, UUID userId) {
        return jpaRepository.findByIdAndUserId(paymentId, userId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findLockedById(UUID paymentId) {
        return jpaRepository.findLockedById(paymentId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findLockedByOrderId(UUID orderId) {
        return jpaRepository.findLockedByOrderId(orderId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findLockedByOrderIdAndUserId(UUID orderId, UUID userId) {
        return jpaRepository.findLockedByOrderIdAndUserId(orderId, userId)
                .map(PaymentJpaEntity::toDomain);
    }
}
