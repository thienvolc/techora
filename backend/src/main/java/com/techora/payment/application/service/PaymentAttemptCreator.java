package com.techora.payment.application.service;

import com.techora.payment.application.policy.VnPayAttemptExpiryPolicy;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.service.PaymentReferenceGenerator;
import com.techora.payment.domain.valueobject.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentAttemptCreator {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentReferenceGenerator paymentReferenceGenerator;
    private final VnPayAttemptExpiryPolicy vnPayAttemptExpiryPolicy;
    private final Clock clock;

    public PaymentAttempt getOrCreatePendingAttempt(Payment payment, PaymentProvider provider) {
        return paymentAttemptRepository.findLockedReusablePendingByPaymentId(payment.getId(), now())
                .orElseGet(() -> createPendingAttempt(payment, provider));
    }

    public PaymentAttempt createPendingAttempt(Payment payment, PaymentProvider provider) {
        Instant createdAt = now();
        PaymentAttempt attempt = PaymentAttempt.createPending(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                provider,
                paymentReferenceGenerator.generate(),
                payment.getAmount(),
                vnPayAttemptExpiryPolicy.attemptExpiresAt(createdAt),
                createdAt
        );
        return paymentAttemptRepository.save(attempt);
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
