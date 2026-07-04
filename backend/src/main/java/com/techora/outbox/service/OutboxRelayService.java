package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxRelayOutcome;
import com.techora.outbox.dto.RelaySummary;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.service.OutboxBatchPublisher.PublishResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    @Value("${app.runtime.instance-id:${random.uuid}}")
    private String instanceId;

    private final OutboxEventClaimer claimer;
    private final OutboxBatchPublisher batchPublisher;

    private final OutboxStateResolver outboxStateResolver;
    private final OutboxStateUpdater outboxStateUpdater;

    public RelaySummary relayPendingEvents(@NonNull List<OutboxEventType> eventTypes, int batchSize) {
        if (eventTypes.isEmpty()) {
            return RelaySummary.empty();
        }

        List<OutboxEventEntity> claimedEvents = claimer.claim(eventTypes, batchSize, instanceId);
        if (claimedEvents.isEmpty()) {
            return RelaySummary.empty();
        }

        List<PublishResult> results = batchPublisher.publishBatch(claimedEvents);
        updateOutcomes(results);
        return RelaySummary.from(results);
    }

    private void updateOutcomes(List<PublishResult> results) {
        List<OutboxRelayOutcome> outcomes = outboxStateResolver.plan(results);
        outboxStateUpdater.apply(outcomes, instanceId);
    }
}
