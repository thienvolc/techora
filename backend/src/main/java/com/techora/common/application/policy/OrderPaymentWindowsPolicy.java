package com.techora.common.application.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class OrderPaymentWindowsPolicy {
    private static final int DEFAULT_WINDOW_TIMEOUT_MINUTES = 60;

    private final Duration timeout;

    public OrderPaymentWindowsPolicy(@Value("${payment.window-timeout-minutes:60}") int timeoutMinutes) {
        int resolvedTimeout = timeoutMinutes <= 0
                ? DEFAULT_WINDOW_TIMEOUT_MINUTES
                : timeoutMinutes;
        this.timeout = Duration.ofMinutes(resolvedTimeout);
    }

    public Instant expiresAt(Instant createdAt) {
        return createdAt.plus(timeout);
    }
}
