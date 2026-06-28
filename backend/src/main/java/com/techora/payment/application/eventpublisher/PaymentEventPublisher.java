package com.techora.payment.application.eventpublisher;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;

public interface PaymentEventPublisher {

    void publish(PaymentConfirmedEvent event);

    void publish(PaymentFailedEvent event);

    void publish(PaymentReconciliationRequiredEvent event);
}
