package com.techora.payment.application.service;

import com.techora.payment.application.port.order.PreparedOrderForPayment;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentCreator {
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public Payment createPending(PreparedOrderForPayment preparedOrder) {
        return paymentRepository.save(buildPendingPayment(preparedOrder));
    }

    private Payment buildPendingPayment(PreparedOrderForPayment preparedOrder) {
        return Payment.createPending(
                preparedOrder.orderId(),
                preparedOrder.userId(),
                preparedOrder.username(),
                preparedOrder.total(),
                preparedOrder.paymentDeadlineAt(),
                now()
        );
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
