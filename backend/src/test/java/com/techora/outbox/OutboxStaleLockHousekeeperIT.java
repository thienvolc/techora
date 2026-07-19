package com.techora.outbox;

import com.techora.common.infra.config.prop.OutboxRetryProperties;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.outbox.scheduler.OutboxStaleLockHousekeeper;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxStaleLockHousekeeperIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxStaleLockHousekeeper housekeeper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxRetryProperties retryProperties;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void staleProcessingRowsAreReleasedBackToPending() {
        Instant now = Instant.now();
        OutboxEventEntity staleProcessingEvent = processingEvent(
                "dead-worker",
                now.minusSeconds(retryProperties.processingTimeoutSeconds() + 10)
        );
        OutboxEventEntity activeProcessingEvent = processingEvent("active-worker", now);

        outboxEventRepository.saveAll(List.of(staleProcessingEvent, activeProcessingEvent));
        Instant beforeHousekeeping = Instant.now();

        housekeeper.releaseStaleEvents();

        assertStaleEventReleased(staleProcessingEvent, beforeHousekeeping);
        assertActiveEventStillProcessing(activeProcessingEvent);
    }

    private OutboxEventEntity processingEvent(String lockedBy, Instant lockedAt) {
        Instant persistedLockTime = lockedAt.truncatedTo(ChronoUnit.MICROS);
        OutboxEventEntity event = TestFixtures.pendingPaymentOutboxEvent(
                UUID.randomUUID(),
                OutboxEventType.PAYMENT_CONFIRMED
        );
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setLockedBy(lockedBy);
        event.setLockedAt(persistedLockTime);
        event.setUpdatedAt(persistedLockTime);

        return event;
    }

    private void assertStaleEventReleased(OutboxEventEntity staleProcessingEvent,
                                          Instant beforeHousekeeping) {
        OutboxEventEntity releasedEvent = outboxEventRepository.findById(staleProcessingEvent.getId()).orElseThrow();

        assertThat(releasedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(releasedEvent.getLockedAt()).isNull();
        assertThat(releasedEvent.getLockedBy()).isNull();
        assertThat(releasedEvent.getNextAttemptAt()).isAfterOrEqualTo(beforeHousekeeping);
        assertThat(releasedEvent.getRetryCount()).isEqualTo(staleProcessingEvent.getRetryCount());
        assertThat(releasedEvent.getProcessedAt()).isNull();
        assertThat(releasedEvent.getFailedAt()).isNull();
    }

    private void assertActiveEventStillProcessing(OutboxEventEntity activeProcessingEvent) {
        OutboxEventEntity processingEvent = outboxEventRepository.findById(activeProcessingEvent.getId()).orElseThrow();

        assertThat(processingEvent.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(processingEvent.getLockedAt()).isEqualTo(activeProcessingEvent.getLockedAt());
        assertThat(processingEvent.getLockedBy()).isEqualTo(activeProcessingEvent.getLockedBy());
    }
}
