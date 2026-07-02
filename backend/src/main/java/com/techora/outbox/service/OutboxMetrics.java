package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxMetrics {

    private static final String EVENT_TYPE_TAG = "eventType";
    private static final String EXCEPTION_TAG = "exception";
    private static final String PUBLISHED_METRIC = "techora.outbox.published";
    private static final String RETRY_SCHEDULED_METRIC = "techora.outbox.retry_scheduled";
    private static final String FAILED_TERMINAL_METRIC = "techora.outbox.failed_terminal";
    private static final String HANDLER_ERROR_METRIC = "techora.outbox.handler_error";
    private static final String STALE_RELEASED_METRIC = "techora.outbox.stale_processing_released";

    private final MeterRegistry meterRegistry;

    public void recordPublished(OutboxEventType eventType) {
        meterRegistry.counter(PUBLISHED_METRIC, EVENT_TYPE_TAG, eventType.name()).increment();
    }

    public void recordRetryScheduled(OutboxEventType eventType) {
        meterRegistry.counter(RETRY_SCHEDULED_METRIC, EVENT_TYPE_TAG, eventType.name()).increment();
    }

    public void recordTerminalFailure(OutboxEventType eventType) {
        meterRegistry.counter(FAILED_TERMINAL_METRIC, EVENT_TYPE_TAG, eventType.name()).increment();
    }

    public void recordPublishError(OutboxEventType eventType, RuntimeException ex) {
        meterRegistry.counter(
                        HANDLER_ERROR_METRIC,
                        EVENT_TYPE_TAG, eventType.name(),
                        EXCEPTION_TAG, ex.getClass().getSimpleName())
                .increment();
    }

    public void recordStaleReleased(int count) {
        meterRegistry.counter(STALE_RELEASED_METRIC).increment(count);
    }
}
