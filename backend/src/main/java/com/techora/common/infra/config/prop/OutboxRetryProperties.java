package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.retry")
public record OutboxRetryProperties(
        int maxRetries,
        long maxBackoffSeconds,
        long stepBackoffSeconds,
        long processingTimeoutSeconds
) {
    public OutboxRetryProperties {
        maxRetries = maxRetries <= 0 ? 5 : maxRetries;
        maxBackoffSeconds = maxBackoffSeconds <= 0 ? 60L : maxBackoffSeconds;
        stepBackoffSeconds = stepBackoffSeconds <= 0 ? 5L : stepBackoffSeconds;
        processingTimeoutSeconds = processingTimeoutSeconds <= 0 ? 300L : processingTimeoutSeconds;
    }
}
