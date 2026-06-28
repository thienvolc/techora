package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {
    private static final String EVENT_TYPE_TAG = "eventType";
    private static final String EXCEPTION_TAG = "exception";
    private static final String PUBLISHED_METRIC = "techora.outbox.published";
    private static final String RETRY_SCHEDULED_METRIC = "techora.outbox.retry_scheduled";
    private static final String FAILED_TERMINAL_METRIC = "techora.outbox.failed_terminal";
    private static final String HANDLER_ERROR_METRIC = "techora.outbox.handler_error";
    private static final String STALE_RELEASED_METRIC = "techora.outbox.stale_processing_released";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 4_000;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRetryPolicy retryPolicy;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${app.runtime.instance-id:${random.uuid}}")
    private String instanceId;

    public int publishPendingEvents(int batchSize, Consumer<OutboxEventEntity> publisher) {
        List<OutboxEventEntity> events = claimReadyEvents(null, batchSize);
        events.forEach(event -> publishEvent(event, publisher));
        return events.size();
    }

    public int publishPendingEvents(OutboxEventType eventType,
                                    int batchSize,
                                    Consumer<OutboxEventEntity> publisher) {

        List<OutboxEventEntity> events = claimReadyEvents(eventType, batchSize);
        events.forEach(event -> publishEvent(event, publisher));
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
            meterRegistry.counter(STALE_RELEASED_METRIC).increment(releasedEvents);
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

    private void publishEvent(OutboxEventEntity event, Consumer<OutboxEventEntity> publisher) {
        try {
            publisher.accept(event);
            markPublished(event);
        } catch (RuntimeException ex) {
            recordHandlerError(event, ex);
            markRetryOrFailed(event, ex);
        }
    }

    private void markPublished(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(event.getId())
                .filter(processingEvent -> processingEvent.getStatus() == OutboxEventStatus.PROCESSING)
                .ifPresent(processingEvent -> {
                    processingEvent.markPublished(Instant.now());
                    meterRegistry.counter(PUBLISHED_METRIC, EVENT_TYPE_TAG, processingEvent.getEventType().name())
                            .increment();
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
                        meterRegistry.counter(RETRY_SCHEDULED_METRIC, EVENT_TYPE_TAG, processingEvent.getEventType().name())
                                .increment();
                        return;
                    }

                    processingEvent.markFailed(errorMessage, now);
                    meterRegistry.counter(FAILED_TERMINAL_METRIC, EVENT_TYPE_TAG, processingEvent.getEventType().name())
                            .increment();
                }));
    }

    private void recordHandlerError(OutboxEventEntity event, RuntimeException ex) {
        meterRegistry.counter(
                        HANDLER_ERROR_METRIC,
                        EVENT_TYPE_TAG, event.getEventType().name(),
                        EXCEPTION_TAG, ex.getClass().getSimpleName())
                .increment();
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
