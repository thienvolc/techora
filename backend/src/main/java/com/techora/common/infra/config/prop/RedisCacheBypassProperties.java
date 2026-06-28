package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache.redis-bypass")
public record RedisCacheBypassProperties(
        boolean enabled,
        int failureThreshold,
        Duration cooldown
) {

    public RedisCacheBypassProperties {
        if (failureThreshold <= 0) {
            failureThreshold = 3;
        }
        cooldown = positiveOrDefault(cooldown, Duration.ofSeconds(5));
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative()
                ? defaultValue
                : value;
    }
}
