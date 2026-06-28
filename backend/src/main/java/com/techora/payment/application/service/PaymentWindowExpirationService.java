package com.techora.payment.application.service;

import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.application.result.PaymentWindowExpirationResult;
import com.techora.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWindowExpirationService {
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public PaymentWindowExpirationResult expireByOrderId(UUID orderId) {
        return paymentRepository.findLockedByOrderId(orderId)
                .map(this::expire)
                .orElse(PaymentWindowExpirationResult.NOT_FOUND);
    }

    private PaymentWindowExpirationResult expire(Payment payment) {
        if (payment.isPaid()) {
            return PaymentWindowExpirationResult.ALREADY_PAID;
        }

        if (!payment.isPending()) {
            return PaymentWindowExpirationResult.NOT_EXPIRABLE;
        }

        payment.markExpired(Instant.now(clock));
        paymentRepository.save(payment);
        return PaymentWindowExpirationResult.EXPIRED;
    }
}
