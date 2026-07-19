package com.techora.outbox.scheduler;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.policy.OutboxRetryPolicy;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.outbox.service.OutboxMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxStaleLockHousekeeper {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRetryPolicy retryPolicy;
    private final OutboxMetrics outboxMetrics;

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.housekeeping-delay-ms:2000}")
    public void releaseStaleEvents() {
        Instant now = Instant.now();
        Instant threshold = retryPolicy.saleProcessingBefore(now);

        int releasedEvents = outboxEventRepository.releaseStaleProcessingEvents(
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING,
                threshold,
                now
        );

        if (releasedEvents > 0) {
            log.info("Housekeeper recovered {} stale outbox events back to PENDING.", releasedEvents);
            outboxMetrics.recordStaleReleased(releasedEvents);
        }
    }
}
