package com.techora.payment.application.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class VnPayAttemptExpiryPolicy {
    private static final int DEFAULT_ATTEMPT_TIMEOUT_MINUTES = 15;

    private final Duration attemptTimeout;

    public VnPayAttemptExpiryPolicy(
            @Value("${payment.providers.vnpay.payment-timeout-minutes:15}") int attemptTimeoutMinutes) {

        int resolvedAttemptTimeout = attemptTimeoutMinutes <= 0
                ? DEFAULT_ATTEMPT_TIMEOUT_MINUTES
                : attemptTimeoutMinutes;
        this.attemptTimeout = Duration.ofMinutes(resolvedAttemptTimeout);
    }

    public Instant attemptExpiresAt(Instant createdAt) {
        return createdAt.plus(attemptTimeout);
    }
}
