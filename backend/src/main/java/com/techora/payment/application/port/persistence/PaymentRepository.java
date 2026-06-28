package com.techora.payment.application.port.persistence;

import com.techora.payment.domain.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    Optional<Payment> findByIdAndUserId(UUID paymentId, UUID userId);

    Optional<Payment> findLockedById(UUID paymentId);

    Optional<Payment> findLockedByOrderId(UUID orderId);

    Optional<Payment> findLockedByOrderIdAndUserId(UUID orderId, UUID userId);
}
