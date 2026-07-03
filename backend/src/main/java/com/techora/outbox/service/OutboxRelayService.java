package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.constant.OutboxRelayOutcomeType;
import com.techora.outbox.dto.OutboxRelayOutcome;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventBulkUpdater;
import com.techora.outbox.repository.OutboxEventRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.techora.outbox.constant.OutboxRelayOutcomeType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final OutboxEventClaimer claimer;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxMetrics outboxMetrics;
    private final OutboxEventBulkUpdater bulkUpdater;
    private final OutboxBatchPublisher batchPublisher;
    private final OutboxRelayOutcomePlanner outboxRelayOutcomePlanner;

    @Value("${app.runtime.instance-id:${random.uuid}}")
    private String instanceId;

    @Transactional
    public boolean retryFailedEvent(UUID eventId) {
        return outboxEventRepository.findById(eventId)
                .filter(OutboxEventEntity::isFailed)
                .map(event -> {
                    event.retryNow(Instant.now());
                    return true;
                })
                .orElse(false);
    }

    public int relayPendingEvents(@NonNull List<OutboxEventType> eventTypes, int batchSize) {
        if (eventTypes.isEmpty()) {
            return -1;
        }

        List<OutboxEventEntity> events = claimer.claim(eventTypes, batchSize);
        if (events.isEmpty()) {
            return 0;
        }

        List<OutboxBatchPublisher.PublishResult> results = batchPublisher.publishBatch(events);
        markOutcomes(results);
        return events.size();
    }


    private void markOutcomes(List<OutboxBatchPublisher.PublishResult> results) {
        List<OutboxRelayOutcome> outcomes = outboxRelayOutcomePlanner.plan(results);

        transactionTemplate.executeWithoutResult(status -> {
            applyOutcomes(outcomes);
        });
    }

    private void applyOutcomes(List<OutboxRelayOutcome> outcomes) {
        List<OutboxRelayOutcome> published = filter(outcomes, PUBLISHED);
        List<OutboxRelayOutcome> retries = filter(outcomes, RETRY_SCHEDULED);
        List<OutboxRelayOutcome> failures = filter(outcomes, FAILED);

        markPublished(published);
        scheduleRetries(retries);
        markFailed(failures);
    }

    private void markPublished(List<OutboxRelayOutcome> outcomes) {
        String lockedBy = instanceId;
        int updatedRows = bulkUpdater.markPublished(outcomes, lockedBy);

        if (updatedRows != outcomes.size()) {
            log.warn(
                    "Outbox relay marked fewer published events than expected. expected={}, updated={}, worker={}",
                    outcomes.size(),
                    updatedRows,
                    instanceId
            );
        }

        outcomes.forEach(o -> outboxMetrics.recordPublished(o.eventType()));
    }

    private void scheduleRetries(List<OutboxRelayOutcome> outcomes) {
        String lockedBy = instanceId;
        int[] updateCounts = bulkUpdater.scheduleRetries(outcomes, lockedBy);

        for (int i = 0; i < updateCounts.length; i++) {
            OutboxRelayOutcome outcome = outcomes.get(i);
            if (updateCounts[i] == 0) {
                logOutcomeNotApplied(outcome, "scheduleRetry");
                continue;
            }
            outboxMetrics.recordRetryScheduled(outcome.eventType());
        }
    }

    private void markFailed(List<OutboxRelayOutcome> outcomes) {
        String lockedBy = instanceId;
        int[] updateCounts = bulkUpdater.markFailed(outcomes, lockedBy);

        for (int i = 0; i < updateCounts.length; i++) {
            OutboxRelayOutcome outcome = outcomes.get(i);
            if (updateCounts[i] == 0) {
                logOutcomeNotApplied(outcome, "markFailed");
                continue;
            }
            outboxMetrics.recordTerminalFailure(outcome.eventType());
        }
    }

    private void logOutcomeNotApplied(OutboxRelayOutcome outcome, String operation) {
        log.warn(
                "Outbox relay outcome was not applied. operation={}, outboxId={}, eventType={}, outcomeType={}, worker={}. " +
                        "The event is no longer PROCESSING for this worker or its state changed concurrently.",
                operation,
                outcome.eventId(),
                outcome.eventType(),
                outcome.type(),
                instanceId
        );
    }

    private List<OutboxRelayOutcome> filter(List<OutboxRelayOutcome> outcomes, OutboxRelayOutcomeType type) {
        return outcomes.stream()
                .filter(o -> o.type() == type)
                .toList();
    }

}
