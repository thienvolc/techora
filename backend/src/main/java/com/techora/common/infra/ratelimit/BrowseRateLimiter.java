package com.techora.common.infra.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.techora.common.infra.config.prop.BrowseRateLimitProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class BrowseRateLimiter {

    private static final String REJECTED_METRIC = "techora.rate_limit.rejected";
    private static final String PERMITTED_METRIC = "techora.rate_limit.permitted";

    private final BrowseRateLimitProperties properties;
    private final MeterRegistry meterRegistry;
    private final RateLimiterConfig rateLimiterConfig;
    private final Cache<String, RateLimiter> rateLimiters;

    public BrowseRateLimiter(BrowseRateLimitProperties properties,
                             MeterRegistry meterRegistry) {

        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(properties.limitForPeriod())
                .limitRefreshPeriod(properties.limitRefreshPeriod())
                .timeoutDuration(properties.timeoutDuration())
                .writableStackTraceEnabled(false)
                .build();
        this.rateLimiters = Caffeine.newBuilder()
                .maximumSize(properties.maximumClients())
                .expireAfterAccess(clientEntryTtl(properties.limitRefreshPeriod()))
                .build();
    }

    public boolean isAllowed(String clientKey) {
        if (!properties.enabled()) {
            return true;
        }

        boolean allowed = rateLimiters.get(clientKey, this::createRateLimiter)
                .acquirePermission();
        meterRegistry.counter(
                        allowed ? PERMITTED_METRIC : REJECTED_METRIC,
                        "policy", "browse")
                .increment();
        return allowed;
    }

    public long retryAfterSeconds() {
        return Math.max(1, properties.limitRefreshPeriod().toSeconds());
    }

    private RateLimiter createRateLimiter(String clientKey) {
        return RateLimiter.of("browse-" + clientKey, rateLimiterConfig);
    }

    private Duration clientEntryTtl(Duration refreshPeriod) {
        return refreshPeriod.multipliedBy(10).compareTo(Duration.ofMinutes(5)) < 0
                ? Duration.ofMinutes(5)
                : refreshPeriod.multipliedBy(10);
    }
}
