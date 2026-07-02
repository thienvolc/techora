package com.techora.payment.application.port.persistence;

import com.techora.payment.domain.entity.PaymentAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository {

    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt> findById(UUID attemptId);

    Optional<PaymentAttempt> findLatestByPaymentId(UUID paymentId);

    Optional<PaymentAttempt> findLockedReusablePendingByPaymentId(UUID paymentId, Instant now);

    Optional<PaymentAttempt> findByProviderReference(String providerReference);

    Optional<PaymentAttempt> findLockedById(UUID attemptId);

    List<PaymentAttempt> findExpiredPendingForUpdate(Instant now, int limit);

    Page<PaymentAttempt> findUnresolvedReconciliationRequired(Pageable pageable);
}
