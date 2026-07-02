package com.techora.payment.infra.outbox;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentOutboxListener {

    private final PaymentOutboxRecorder outboxRecorder;

    @EventListener
    public void on(PaymentConfirmedEvent event) {
        outboxRecorder.record(event);
    }

    @EventListener
    public void on(PaymentFailedEvent event) {
        outboxRecorder.record(event);
    }

    @EventListener
    public void on(PaymentReconciliationRequiredEvent event) {
        outboxRecorder.record(event);
    }
}
