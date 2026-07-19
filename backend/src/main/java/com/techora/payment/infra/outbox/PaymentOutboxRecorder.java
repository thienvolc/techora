package com.techora.payment.infra.outbox;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.dto.OutboxHeaders;
import com.techora.outbox.port.OutboxEventPort;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import com.techora.payment.infra.outbox.schema.PaymentConfirmedPayload;
import com.techora.payment.infra.outbox.schema.PaymentEventPayload;
import com.techora.payment.infra.outbox.schema.PaymentFailedPayload;
import com.techora.payment.infra.outbox.schema.PaymentReconciliationRequiredPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentOutboxRecorder {

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
                PaymentConfirmedPayload.fromEvent(event)
        );
    }

    public void record(PaymentFailedEvent event) {
        appendOutboxRecord(
                event.paymentId(),
                event.orderId(),
                OutboxEventType.PAYMENT_FAILED,
                event.eventVersion(),
                PaymentFailedPayload.fromEvent(event)
        );
    }

    public void record(PaymentReconciliationRequiredEvent event) {
        appendOutboxRecord(
                event.paymentId(),
                event.orderId(),
                OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED,
                event.eventVersion(),
                PaymentReconciliationRequiredPayload.fromEvent(event)
        );
    }

    private void appendOutboxRecord(UUID paymentId,
                                    UUID orderId,
                                    OutboxEventType eventType,
                                    int eventVersion,
                                    PaymentEventPayload payload) {

        var record = OutboxEventRecord.builder()
                .aggregateType(OutboxAggregateType.PAYMENT)
                .aggregateId(paymentId)
                .eventType(eventType)
                .topic(paymentEventsTopic)
                .messageKey(orderId.toString())
                .eventVersion(eventVersion)
                .occurredAt(payload.occurredAt())
                .headers(OutboxHeaders.of(PaymentOutboxHeaders.values()))
                .data(payload)
                .build();
        outboxEventPort.append(record);
    }
}
