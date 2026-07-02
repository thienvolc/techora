package com.techora.payment.application.service.order;

import com.techora.payment.application.model.PendingPaymentExpirationResult;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingPaymentExpirationService {
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional
    public PendingPaymentExpirationResult expireForOrderTimeout(UUID orderId) {
        return paymentRepository.findLockedByOrderId(orderId)
                .map(this::expirePayment)
                .orElse(PendingPaymentExpirationResult.NOT_FOUND);
    }

    private PendingPaymentExpirationResult expirePayment(Payment payment) {
        if (payment.isPaid()) {
            return PendingPaymentExpirationResult.ALREADY_PAID;
        }

        if (!payment.isPending()) {
            return PendingPaymentExpirationResult.NOT_PENDING;
        }

        payment.markExpired(Instant.now(clock));
        paymentRepository.save(payment);
        return PendingPaymentExpirationResult.EXPIRED;
    }
}
