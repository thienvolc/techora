package com.techora.payment.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdAndUserId(UUID paymentId, UUID userId);

    Optional<PaymentJpaEntity> findByProviderReference(String providerReference);
}
