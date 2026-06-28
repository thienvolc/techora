package com.techora.outbox.service;

import com.techora.common.infra.config.prop.OutboxRetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxRetryPolicy {
    private final OutboxRetryProperties properties;

    public boolean canRetry(int nextRetryCount) {
        return nextRetryCount <= properties.maxRetries();
    }

    public Instant nextAttemptAt(int nextRetryCount, Instant now) {
        long delaySeconds = Math.min(
                nextRetryCount * properties.stepBackoffSeconds(),
                properties.maxBackoffSeconds()
        );
        return now.plusSeconds(delaySeconds);
    }

    public Instant staleProcessingBefore(Instant now) {
        return now.minusSeconds(properties.processingTimeoutSeconds());
    }
}
