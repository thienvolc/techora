package com.techora.payment.application.port.persistence;

import com.techora.payment.domain.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByIdAndUserId(UUID paymentId, UUID userId);

    Optional<Payment> findByProviderReference(String providerReference);
}
