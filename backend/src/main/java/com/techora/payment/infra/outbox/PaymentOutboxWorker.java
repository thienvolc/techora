package com.techora.payment.infra.outbox;

import com.techora.common.infra.service.JsonCodec;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxPayload;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.service.OutboxPublisherService;
import com.techora.payment.application.port.order.OrderPaymentConfirmationResult;
import com.techora.payment.application.port.order.OrderPaymentPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxWorker {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String ORDER_ID = "orderId";
    private static final String PAYMENT_ID = "paymentId";
    private static final String PROVIDER_NAME = "providerName";
    private static final String REASON = "reason";
    private static final String UNKNOWN_PROVIDER = "UNKNOWN";
    private static final String UNKNOWN_REASON = "UNKNOWN";
    private static final String RECONCILIATION_REQUIRED_METRIC = "techora.payment.reconciliation_required";

    private final OutboxPublisherService outboxPublisherService;
    private final OrderPaymentPort orderPaymentPort;
    private final JsonCodec jsonCodec;
    private final MeterRegistry meterRegistry;

    @Value("${app.outbox.payment-worker.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.payment-worker.delay-ms:${app.outbox.worker-delay-ms:2000}}")
    @SchedulerLock(
            name = "paymentOutboxWorker.publishPaymentEvents",
            lockAtMostFor = "${app.outbox.payment-worker.lock-at-most-for:PT30S}"
    )
    public int publishPaymentEvents() {
        int confirmed = outboxPublisherService.publishPendingEvents(
                OutboxEventType.PAYMENT_CONFIRMED,
                resolvedBatchSize(),
                this::handleConfirmedPayment
        );

        int failed = outboxPublisherService.publishPendingEvents(
                OutboxEventType.PAYMENT_FAILED,
                resolvedBatchSize(),
                this::handleFailedPayment
        );

        int reconciliationRequired = outboxPublisherService.publishPendingEvents(
                OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED,
                resolvedBatchSize(),
                this::handleReconciliationRequired
        );

        return confirmed + failed + reconciliationRequired;
    }

    private void handleConfirmedPayment(OutboxEventEntity event) {
        Map<String, Object> attributes = readAttributes(event);
        UUID orderId = readUuid(attributes, ORDER_ID);
        String providerName = readString(attributes, PROVIDER_NAME, UNKNOWN_PROVIDER);
        OrderPaymentConfirmationResult result = orderPaymentPort.confirmPayment(orderId, providerName);
        if (result == OrderPaymentConfirmationResult.NOT_PAYABLE) {
            log.warn("Payment confirmed but order is not payable. orderId={}, provider={}", orderId, providerName);
        }
    }

    private void handleFailedPayment(OutboxEventEntity event) {
        // Payment owns provider failures. Order stays PAYMENT_PENDING for retry or expiry policy.
    }

    private void handleReconciliationRequired(OutboxEventEntity event) {
        Map<String, Object> attributes = readAttributes(event);
        String paymentId = readString(attributes, PAYMENT_ID, String.valueOf(event.getAggregateId()));
        String reason = readString(attributes, REASON, UNKNOWN_REASON);
        meterRegistry.counter(RECONCILIATION_REQUIRED_METRIC, REASON, reason).increment();
        log.warn("Payment reconciliation required. paymentId={}, reason={}", paymentId, reason);
    }

    private Map<String, Object> readAttributes(OutboxEventEntity event) {
        return jsonCodec.fromJson(event.getPayload(), OutboxPayload.class).attributes();
    }

    private UUID readUuid(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing outbox attribute: " + key);
        }
        return UUID.fromString(String.valueOf(value));
    }

    private String readString(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? defaultValue : stringValue;
    }

    private int resolvedBatchSize() {
        return batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }
}
