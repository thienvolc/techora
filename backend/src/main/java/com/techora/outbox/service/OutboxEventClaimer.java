package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.policy.OutboxRetryPolicy;
import com.techora.outbox.repository.OutboxEventRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimer {

    @Value("${app.runtime.instance-id:${random.uuid}}")
    private String instanceId;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRetryPolicy retryPolicy;
    private final OutboxMetrics outboxMetrics;

    private final TransactionTemplate transactionTemplate;

    public List<OutboxEventEntity> claim(@NonNull List<OutboxEventType> eventTypes, int batchSize) {
        if (eventTypes.isEmpty()) {
            return List.of();
        }

        List<String> eventTypeNames = getEventTypeNames(eventTypes);
        String lockedBy = instanceId;

        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();

            releaseStaleProcessingEvents(now);

            return outboxEventRepository.claimReadyEventsByTypes(
                    OutboxEventStatus.PENDING.name(),
                    OutboxEventStatus.PROCESSING.name(),
                    eventTypeNames,
                    now,
                    lockedBy,
                    batchSize
            );
        });
    }

    private void releaseStaleProcessingEvents(Instant now) {
        int releasedEvents = outboxEventRepository.releaseStaleProcessingEvents(
                OutboxEventStatus.PROCESSING.name(),
                OutboxEventStatus.PENDING.name(),
                retryPolicy.staleProcessingBefore(now),
                now
        );
        if (releasedEvents > 0) {
            outboxMetrics.recordStaleReleased(releasedEvents);
        }
    }

    private List<String> getEventTypeNames(Collection<OutboxEventType> eventTypes) {
        return eventTypes.stream()
                .map(OutboxEventType::name)
                .toList();
    }

}
