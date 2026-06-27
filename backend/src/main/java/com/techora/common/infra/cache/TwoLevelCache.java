package com.techora.common.infra.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class TwoLevelCache implements Cache {

    private static final String CACHE_TAG = "cache";
    private static final String LOCAL_HIT_METRIC = "techora.cache.local.hit";
    private static final String LOCAL_MISS_METRIC = "techora.cache.local.miss";
    private static final String REDIS_HIT_METRIC = "techora.cache.redis.hit";
    private static final String REDIS_MISS_METRIC = "techora.cache.redis.miss";

    private final Cache localCache;
    private final Cache redisCache;
    private final MeterRegistry meterRegistry;

    public TwoLevelCache(Cache localCache, Cache redisCache, MeterRegistry meterRegistry) {
        this.localCache = localCache;
        this.redisCache = redisCache;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getName() {
        return redisCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return List.of(localCache.getNativeCache(), redisCache.getNativeCache());
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper localValue = localCache.get(key);
        if (localValue != null) {
            record(LOCAL_HIT_METRIC);
            return localValue;
        }
        record(LOCAL_MISS_METRIC);

        ValueWrapper redisValue = redisCache.get(key);
        if (redisValue != null) {
            record(REDIS_HIT_METRIC);
            localCache.put(key, redisValue.get());
        } else {
            record(REDIS_MISS_METRIC);
        }
        return redisValue;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T localValue = localCache.get(key, type);
        if (localValue != null) {
            record(LOCAL_HIT_METRIC);
            return localValue;
        }
        record(LOCAL_MISS_METRIC);

        T redisValue = redisCache.get(key, type);
        if (redisValue != null) {
            record(REDIS_HIT_METRIC);
            localCache.put(key, redisValue);
        } else {
            record(REDIS_MISS_METRIC);
        }
        return redisValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper cachedValue = get(key);
        if (cachedValue != null) {
            return (T) cachedValue.get();
        }

        try {
            T loadedValue = valueLoader.call();
            put(key, loadedValue);
            return loadedValue;
        } catch (Throwable ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        ValueWrapper cachedValue = get(key);
        return cachedValue == null
                ? null
                : CompletableFuture.completedFuture(cachedValue.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        ValueWrapper cachedValue = get(key);
        if (cachedValue != null) {
            return CompletableFuture.completedFuture((T) cachedValue.get());
        }

        CompletableFuture<T> loadedValue = valueLoader.get();
        loadedValue.thenAccept(value -> {
            if (value != null) {
                put(key, value);
            }
        });
        return loadedValue;
    }

    @Override
    public void put(Object key, Object value) {
        localCache.put(key, value);
        redisCache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper cachedValue = get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        localCache.evict(key);
        redisCache.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean localEvicted = localCache.evictIfPresent(key);
        boolean redisEvicted = redisCache.evictIfPresent(key);
        return localEvicted || redisEvicted;
    }

    @Override
    public void clear() {
        localCache.clear();
        redisCache.clear();
    }

    @Override
    public boolean invalidate() {
        boolean localInvalidated = localCache.invalidate();
        boolean redisInvalidated = redisCache.invalidate();
        return localInvalidated || redisInvalidated;
    }

    private void record(String metricName) {
        meterRegistry.counter(metricName, CACHE_TAG, getName())
                .increment();
    }
}
