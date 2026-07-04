package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxRelayOutcomeType;
import com.techora.outbox.dto.OutboxRelayOutcome;
import com.techora.outbox.repository.OutboxEventBulkUpdater;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.techora.outbox.constant.OutboxRelayOutcomeType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxStateUpdater {

    private final OutboxMetrics outboxMetrics;
    private final OutboxEventBulkUpdater bulkUpdater;

    @Transactional
    public void apply(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        Map<OutboxRelayOutcomeType, List<OutboxRelayOutcome>> groupedOutcomes = outcomes.stream()
                .collect(Collectors.groupingBy(OutboxRelayOutcome::type));

        List<OutboxRelayOutcome> published = groupedOutcomes.getOrDefault(PUBLISHED, List.of());
        List<OutboxRelayOutcome> retries = groupedOutcomes.getOrDefault(RETRY_SCHEDULED, List.of());
        List<OutboxRelayOutcome> failures = groupedOutcomes.getOrDefault(FAILED, List.of());

        if (!published.isEmpty()) markPublished(published, lockedBy);
        if (!retries.isEmpty()) scheduleRetries(retries, lockedBy);
        if (!failures.isEmpty()) markFailed(failures, lockedBy);
    }

    private void markPublished(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        List<UUID> updatedIds = bulkUpdater.markPublished(outcomes, lockedBy);
        Set<UUID> updatedIdSet = Set.copyOf(updatedIds);

        for (OutboxRelayOutcome outcome : outcomes) {
            if (!updatedIdSet.contains(outcome.eventId())) {
                processMetricsAndLogs(outcome, Operation.MARK_PUBLISHED, lockedBy);
                continue;
            }
            outboxMetrics.recordPublished(outcome.eventType());
        }
    }

    private void scheduleRetries(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        int[] updateCounts = bulkUpdater.scheduleRetries(outcomes, lockedBy);

        for (int i = 0; i < updateCounts.length; i++) {
            OutboxRelayOutcome outcome = outcomes.get(i);
            if (updateCounts[i] == 0) {
                processMetricsAndLogs(outcome, Operation.SCHEDULE_RETRY, lockedBy);
                continue;
            }
            outboxMetrics.recordRetryScheduled(outcome.eventType());
        }
    }

    private void markFailed(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        int[] updateCounts = bulkUpdater.markFailed(outcomes, lockedBy);

        for (int i = 0; i < updateCounts.length; i++) {
            OutboxRelayOutcome outcome = outcomes.get(i);
            if (updateCounts[i] == 0) {
                processMetricsAndLogs(outcome, Operation.MARK_FAILED, lockedBy);
                continue;
            }
            outboxMetrics.recordTerminalFailure(outcome.eventType());
        }
    }

    private void processMetricsAndLogs(OutboxRelayOutcome outcome,
                                       Operation operation,
                                       String lockedBy) {

        log.warn(
                "Outbox relay outcome was not applied. " +
                        "operation={}, outboxId={}, eventType={}, outcomeType={}, worker={}. " +
                        "The event is no longer PROCESSING for this worker or its state changed concurrently.",
                operation.getValue(),
                outcome.eventId(),
                outcome.eventType(),
                outcome.type(),
                lockedBy
        );
    }

    @Getter
    @AllArgsConstructor
    enum Operation {
        MARK_PUBLISHED("markUpdated"),
        SCHEDULE_RETRY("scheduleRetry"),
        MARK_FAILED("markFailed");

        final String value;
    }
}
