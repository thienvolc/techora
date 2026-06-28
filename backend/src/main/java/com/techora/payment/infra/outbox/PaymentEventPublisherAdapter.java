package com.techora.payment.infra.outbox;

import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.payment.application.eventpublisher.PaymentEventPublisher;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisherAdapter implements PaymentEventPublisher {

    private final InternalEventPublisher internalEventPublisher;

    @Override
    public void publish(PaymentConfirmedEvent event) {
        internalEventPublisher.publish(event);
    }

    @Override
    public void publish(PaymentFailedEvent event) {
        internalEventPublisher.publish(event);
    }

    @Override
    public void publish(PaymentReconciliationRequiredEvent event) {
        internalEventPublisher.publish(event);
    }
}
