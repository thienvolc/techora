package com.techora.payment.infra.outbox;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOutboxEventHandler {

    private final PaymentOutboxEventPublisher paymentOutboxEventPublisher;

    @EventListener
    public void on(PaymentConfirmedEvent event) {
        paymentOutboxEventPublisher.recordConfirmed(event);
    }

    @EventListener
    public void on(PaymentFailedEvent event) {
        paymentOutboxEventPublisher.recordFailed(event);
    }
}
