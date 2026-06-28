package com.techora.common.infra.cache;

import com.techora.common.infra.config.prop.RedisCacheBypassProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RedisCacheAvailability {

    private static final String REDIS_BYPASS_OPENED_METRIC = "techora.cache.redis.bypass.opened";

    private final RedisCacheBypassProperties properties;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    private volatile Instant bypassUntil = Instant.EPOCH;

    public RedisCacheAvailability(RedisCacheBypassProperties properties,
                                  MeterRegistry meterRegistry) {

        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public boolean canUseRedis() {
        return !properties.enabled() || Instant.now().isAfter(bypassUntil);
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
    }

    public void recordFailure(RuntimeException exception) {
        if (!properties.enabled()) {
            return;
        }

        if (consecutiveFailures.incrementAndGet() >= properties.failureThreshold()) {
            bypassUntil = Instant.now().plus(properties.cooldown());
            meterRegistry.counter(
                            REDIS_BYPASS_OPENED_METRIC,
                            "exception", exception.getClass().getSimpleName())
                    .increment();
        }
    }
}
