package com.techora.outbox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techora.common.infra.service.JsonCodec;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.policy.OutboxRetryPolicy;
import com.techora.outbox.port.OutboxMessagePublisher;
import com.techora.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxRelayService {
    private static final int MAX_ERROR_MESSAGE_LENGTH = 4_000;
    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<>() {
    };

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRetryPolicy retryPolicy;
    private final TransactionTemplate transactionTemplate;
    private final OutboxMessagePublisher messagePublisher;
    private final OutboxMetrics outboxMetrics;
    private final JsonCodec jsonCodec;

    @Value("${app.runtime.instance-id:${random.uuid}}")
    private String instanceId;

    public int relayPendingEvents(OutboxEventType eventType, int batchSize) {
        List<OutboxEventEntity> events = claimReadyEvents(eventType, batchSize);
        events.forEach(this::relayEvent);
        return events.size();
    }

    public boolean retryFailedEvent(UUID eventId) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> outboxEventRepository.findById(eventId)
                .filter(event -> event.getStatus() == OutboxEventStatus.FAILED)
                .map(event -> {
                    event.retryNow(Instant.now());
                    return true;
                })
                .orElse(false)));
    }

    private List<OutboxEventEntity> claimReadyEvents(OutboxEventType eventType, int batchSize) {
        List<OutboxEventEntity> events = transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            releaseStaleProcessingEvents(now);
            List<OutboxEventEntity> readyEvents = findReadyEvents(eventType, now, resolvedBatchSize(batchSize));
            readyEvents.forEach(event -> event.markProcessing(instanceId, now));
            return readyEvents;
        });
        return events == null ? List.of() : events;
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

    private List<OutboxEventEntity> findReadyEvents(OutboxEventType eventType, Instant now, int batchSize) {
        if (eventType == null) {
            return outboxEventRepository.findReadyEvents(
                    OutboxEventStatus.PENDING.name(),
                    now,
                    batchSize
            );
        }

        return outboxEventRepository.findReadyEventsByType(
                OutboxEventStatus.PENDING.name(),
                eventType.name(),
                now,
                batchSize
        );
    }

    private void relayEvent(OutboxEventEntity event) {
        try {
            messagePublisher.publish(toMessage(event));
            markPublished(event);
        } catch (RuntimeException ex) {
            outboxMetrics.recordPublishError(event.getEventType(), ex);
            markRetryOrFailed(event, ex);
        }
    }

    private void markPublished(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(event.getId())
                .filter(processingEvent -> processingEvent.getStatus() == OutboxEventStatus.PROCESSING)
                .ifPresent(processingEvent -> {
                    processingEvent.markPublished(Instant.now());
                    outboxMetrics.recordPublished(processingEvent.getEventType());
                }));
    }

    private void markRetryOrFailed(OutboxEventEntity event, RuntimeException ex) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(event.getId())
                .filter(processingEvent -> processingEvent.getStatus() == OutboxEventStatus.PROCESSING)
                .ifPresent(processingEvent -> {
                    Instant now = Instant.now();
                    String errorMessage = errorMessage(ex);
                    int nextRetryCount = processingEvent.getRetryCount() + 1;
                    if (retryPolicy.canRetry(nextRetryCount)) {
                        processingEvent.scheduleRetry(
                                errorMessage,
                                retryPolicy.nextAttemptAt(nextRetryCount, now),
                                now
                        );
                        outboxMetrics.recordRetryScheduled(processingEvent.getEventType());
                        return;
                    }

                    processingEvent.markFailed(errorMessage, now);
                    outboxMetrics.recordTerminalFailure(processingEvent.getEventType());
                }));
    }

    private OutboxMessage toMessage(OutboxEventEntity event) {
        return new OutboxMessage(
                event.getEventId(),
                event.getTopic(),
                event.getMessageKey(),
                event.getEventType(),
                event.getEventVersion(),
                headers(event),
                event.getPayload()
        );
    }

    private Map<String, String> headers(OutboxEventEntity event) {
        if (event.getHeaders() == null || event.getHeaders().isBlank()) {
            return Map.of();
        }
        return jsonCodec.fromJson(event.getHeaders(), HEADERS_TYPE);
    }

    private String errorMessage(RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private int resolvedBatchSize(int batchSize) {
        return Math.max(1, batchSize);
    }
}
