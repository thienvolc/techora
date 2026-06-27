package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit.browse")
public record BrowseRateLimitProperties(
        boolean enabled,
        int limitForPeriod,
        Duration limitRefreshPeriod,
        Duration timeoutDuration,
        long maximumClients
) {

    public BrowseRateLimitProperties {
        if (limitForPeriod <= 0) {
            limitForPeriod = 120;
        }
        limitRefreshPeriod = positiveOrDefault(limitRefreshPeriod, Duration.ofMinutes(1));
        timeoutDuration = zeroOrPositiveOrDefault(timeoutDuration, Duration.ZERO);
        if (maximumClients <= 0) {
            maximumClients = 10_000;
        }
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative()
                ? defaultValue
                : value;
    }

    private static Duration zeroOrPositiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isNegative()
                ? defaultValue
                : value;
    }
}
