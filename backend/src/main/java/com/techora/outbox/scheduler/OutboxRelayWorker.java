package com.techora.outbox.scheduler;

import com.techora.common.infra.config.prop.OutboxRelayProperties;
import com.techora.outbox.dto.RelaySummary;
import com.techora.outbox.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayWorker {

    private final OutboxRelayProperties properties;
    private final OutboxRelayService outboxRelayService;

    @Scheduled(fixedDelayString = "${app.outbox.relay.delay-ms:${app.outbox.payment-worker.delay-ms:${app.outbox.worker-delay-ms:2000}}}")
    public void relayPendingEvents() {
        RelaySummary summary = replayBatchesEvents();
        if (summary.hasProcessedAny()) {
            log.info("Relay cycle completed: {} succeeded, {} failed (Total attempted: {})",
                    summary.succeeded(), summary.failed(), summary.attempted());
        }
    }

    public RelaySummary replayBatchesEvents() {
        RelaySummary totalSummary = RelaySummary.empty();

        for (int i = 0; i < properties.maxBatchesPerRun(); i++) {
            RelaySummary batchSummary = processSingleBatch();
            totalSummary = totalSummary.merge(batchSummary);

            if (hasNoMoreEvents(batchSummary)) {
                break;
            }
        }

        return totalSummary;
    }

    private RelaySummary processSingleBatch() {
        return outboxRelayService.relayPendingEvents(properties.eventTypes(), properties.batchSize());
    }

    private boolean hasNoMoreEvents(RelaySummary summary) {
        return summary.isPartial(properties.batchSize());
    }
}
