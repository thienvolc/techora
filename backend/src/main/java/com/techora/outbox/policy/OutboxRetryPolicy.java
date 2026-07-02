package com.techora.outbox.policy;

import com.techora.common.infra.config.prop.OutboxRetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OutboxRetryPolicy {

    private final OutboxRetryProperties properties;

    public boolean canRetry(int failedAttempts) {
        return failedAttempts <= properties.maxRetries();
    }

    public Instant nextAttemptAt(int failedAttempts, Instant now) {
        long delaySeconds = calculateExponentialDelay(failedAttempts);
        return now.plusSeconds(delaySeconds);
    }

    public Instant staleProcessingBefore(Instant now) {
        return now.minusSeconds(properties.processingTimeoutSeconds());
    }

    private long calculateExponentialDelay(int failedAttempts) {
        long baseStep = properties.stepBackoffSeconds();
        long maxBackoff = properties.maxBackoffSeconds();

        long exponentialDelay = baseStep * (long) Math.pow(2, failedAttempts);
        long cappedDelay = Math.min(exponentialDelay, maxBackoff);

        long minDelay = Math.min(baseStep, cappedDelay);

        return ThreadLocalRandom.current().nextLong(minDelay, cappedDelay + 1);
    }
}
