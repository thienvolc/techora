package com.techora.outbox.scheduler;

import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRelayWorker {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String DEFAULT_EVENT_TYPES = "PAYMENT_CONFIRMED,PAYMENT_FAILED,PAYMENT_RECONCILIATION_REQUIRED";

    private final OutboxRelayService outboxRelayService;

    @Value("${app.outbox.relay.batch-size:${app.outbox.payment-worker.batch-size:100}}")
    private int batchSize;

    @Value("${app.outbox.relay.event-types:" + DEFAULT_EVENT_TYPES + "}")
    private String eventTypes;

    @Scheduled(fixedDelayString = "${app.outbox.relay.delay-ms:${app.outbox.payment-worker.delay-ms:${app.outbox.worker-delay-ms:2000}}}")
    public int relayPendingEvents() {
        int resolvedBatchSize = resolveBatchSize();
        return relayEventTypes().stream()
                .mapToInt(eventType -> outboxRelayService.relayPendingEvents(eventType, resolvedBatchSize))
                .sum();
    }

    private List<OutboxEventType> relayEventTypes() {
        return Arrays.stream(eventTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(OutboxEventType::valueOf)
                .toList();
    }

    private int resolveBatchSize() {
        return batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }
}
