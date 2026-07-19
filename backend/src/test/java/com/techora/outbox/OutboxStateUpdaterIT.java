package com.techora.outbox;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxRelayOutcome;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.outbox.service.OutboxStateUpdater;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxStateUpdaterIT extends AbstractIntegrationTest {

    private static final String CURRENT_WORKER = "worker-a";
    private static final String OTHER_WORKER = "worker-b";
    private static final String LOST_OWNERSHIP_METRIC = "techora.outbox.relay.lost_ownership";
    private static final String EVENT_TYPE_TAG = "eventType";
    private static final String OPERATION_TAG = "operation";

    @Autowired
    private OutboxStateUpdater stateUpdater;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void lostOwnershipDuringMarkPublishedIsObservable() {
        OutboxEventEntity processingEvent = seedProcessingEventOwnedByOtherWorker();
        double metricBefore = lostOwnershipCount("mark_published");

        stateUpdater.apply(List.of(OutboxRelayOutcome.published(
                processingEvent.getId(),
                processingEvent.getEventType(),
                Instant.now()
        )), CURRENT_WORKER);

        assertProcessingEventStillOwnedByOtherWorker(processingEvent);
        assertLostOwnershipMetricIncreased("mark_published", metricBefore);
    }

    @Test
    void lostOwnershipDuringScheduleRetryIsObservable() {
        OutboxEventEntity processingEvent = seedProcessingEventOwnedByOtherWorker();
        double metricBefore = lostOwnershipCount("schedule_retry");

        stateUpdater.apply(List.of(OutboxRelayOutcome.retryScheduled(
                processingEvent.getId(),
                processingEvent.getEventType(),
                "Kafka unavailable",
                Instant.now().plusSeconds(1),
                Instant.now()
        )), CURRENT_WORKER);

        assertProcessingEventStillOwnedByOtherWorker(processingEvent);
        assertLostOwnershipMetricIncreased("schedule_retry", metricBefore);
    }

    @Test
    void lostOwnershipDuringMarkFailedIsObservable() {
        OutboxEventEntity processingEvent = seedProcessingEventOwnedByOtherWorker();
        double metricBefore = lostOwnershipCount("mark_failed");

        stateUpdater.apply(List.of(OutboxRelayOutcome.failed(
                processingEvent.getId(),
                processingEvent.getEventType(),
                "Kafka exhausted",
                Instant.now()
        )), CURRENT_WORKER);

        assertProcessingEventStillOwnedByOtherWorker(processingEvent);
        assertLostOwnershipMetricIncreased("mark_failed", metricBefore);
    }

    private OutboxEventEntity seedProcessingEventOwnedByOtherWorker() {
        Instant now = Instant.now();
        OutboxEventEntity event = TestFixtures.pendingPaymentOutboxEvent(
                UUID.randomUUID(),
                OutboxEventType.PAYMENT_CONFIRMED
        );
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setLockedBy(OTHER_WORKER);
        event.setLockedAt(now);
        event.setUpdatedAt(now);

        return outboxEventRepository.save(event);
    }

    private void assertProcessingEventStillOwnedByOtherWorker(OutboxEventEntity originalEvent) {
        OutboxEventEntity event = outboxEventRepository.findById(originalEvent.getId()).orElseThrow();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getLockedBy()).isEqualTo(OTHER_WORKER);
        assertThat(event.getLockedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
        assertThat(event.getProcessedAt()).isNull();
        assertThat(event.getFailedAt()).isNull();
    }

    private void assertLostOwnershipMetricIncreased(String operation, double metricBefore) {
        assertThat(lostOwnershipCount(operation)).isEqualTo(metricBefore + 1.0);
    }

    private double lostOwnershipCount(String operation) {
        return Optional.ofNullable(meterRegistry.find(LOST_OWNERSHIP_METRIC)
                        .tag(EVENT_TYPE_TAG, OutboxEventType.PAYMENT_CONFIRMED.name())
                        .tag(OPERATION_TAG, operation)
                        .counter())
                .map(Counter::count)
                .orElse(0.0);
    }
}
