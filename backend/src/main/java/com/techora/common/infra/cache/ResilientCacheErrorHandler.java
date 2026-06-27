package com.techora.common.infra.cache;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

@Slf4j
@RequiredArgsConstructor
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final String CACHE_ERROR_METRIC = "techora.cache.errors";

    private final MeterRegistry meterRegistry;

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        record("get", cache, key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        record("put", cache, key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        record("evict", cache, key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        record("clear", cache, null, exception);
    }

    private void record(String operation, Cache cache, Object key, RuntimeException exception) {
        meterRegistry.counter(
                        CACHE_ERROR_METRIC,
                        "cache", cache.getName(),
                        "operation", operation,
                        "exception", exception.getClass().getSimpleName())
                .increment();

        log.warn(
                "Cache {} failed for cache={} key={}: {}",
                operation,
                cache.getName(),
                key,
                exception.toString());
    }
}
