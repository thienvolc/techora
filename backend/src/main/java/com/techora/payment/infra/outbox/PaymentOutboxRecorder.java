package com.techora.payment.infra.outbox;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.port.OutboxEventPort;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOutboxRecorder {

    private static final String HEADER_SOURCE_KEY = "source";
    private static final String HEADER_EVENT_TYPE_KEY = "eventType";
    private static final String SOURCE_PAYMENT = "payment";

    private final OutboxEventPort outboxEventPort;
    private final String paymentEventsTopic;

    public PaymentOutboxRecorder(
            OutboxEventPort outboxEventPort,
            @Value("${app.events.payment-topic:techora.payment.events}") String paymentEventsTopic) {
        this.outboxEventPort = outboxEventPort;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    public void record(PaymentConfirmedEvent event) {
        appendOutboxRecord(
                event.paymentId(),
                event.orderId(),
                OutboxEventType.PAYMENT_CONFIRMED,
                event.eventVersion(),
                PaymentOutboxAttributes.confirmed(event)
        );
    }

    public void record(PaymentFailedEvent event) {
        appendOutboxRecord(
                event.paymentId(),
                event.orderId(),
                OutboxEventType.PAYMENT_FAILED,
                event.eventVersion(),
                PaymentOutboxAttributes.failed(event)
        );
    }

    public void record(PaymentReconciliationRequiredEvent event) {
        appendOutboxRecord(
                event.paymentId(),
                event.orderId(),
                OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED,
                event.eventVersion(),
                PaymentOutboxAttributes.reconciliationRequired(event)
        );
    }

    private void appendOutboxRecord(UUID paymentId,
                                    UUID orderId,
                                    OutboxEventType eventType,
                                    int eventVersion,
                                    Map<String, Object> attributes) {

        var record = new OutboxEventRecord(
                OutboxAggregateType.PAYMENT,
                paymentId,
                eventType,
                paymentEventsTopic,
                orderId.toString(),
                eventVersion,
                headers(eventType),
                attributes
        );
        outboxEventPort.append(record);
    }

    private Map<String, String> headers(OutboxEventType eventType) {
        return Map.of(
                HEADER_SOURCE_KEY, SOURCE_PAYMENT,
                HEADER_EVENT_TYPE_KEY, eventType.name()
        );
    }
}
