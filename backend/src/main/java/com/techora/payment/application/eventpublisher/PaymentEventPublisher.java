package com.techora.payment.application.eventpublisher;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;

public interface PaymentEventPublisher {

    void publish(PaymentConfirmedEvent event);

    void publish(PaymentFailedEvent event);
}
