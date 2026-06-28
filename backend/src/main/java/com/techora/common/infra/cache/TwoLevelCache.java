package com.techora.common.infra.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class TwoLevelCache implements Cache {

    private static final String CACHE_TAG = "cache";
    private static final String LOCAL_HIT_METRIC = "techora.cache.local.hit";
    private static final String LOCAL_MISS_METRIC = "techora.cache.local.miss";
    private static final String REDIS_HIT_METRIC = "techora.cache.redis.hit";
    private static final String REDIS_MISS_METRIC = "techora.cache.redis.miss";
    private static final String REDIS_BYPASSED_METRIC = "techora.cache.redis.bypassed";
    private static final String OPERATION_TAG = "operation";
    private static final long MAX_SINGLE_FLIGHT_LOCKS = 10_000;

    private final Cache localCache;
    private final Cache redisCache;
    private final MeterRegistry meterRegistry;
    private final RedisCacheAvailability redisCacheAvailability;
    private final CacheErrorHandler cacheErrorHandler;
    private final com.github.benmanes.caffeine.cache.Cache<Object, ReentrantLock> singleFlightLocks;

    public TwoLevelCache(Cache localCache,
                         Cache redisCache,
                         MeterRegistry meterRegistry,
                         RedisCacheAvailability redisCacheAvailability,
                         CacheErrorHandler cacheErrorHandler) {

        this.localCache = localCache;
        this.redisCache = redisCache;
        this.meterRegistry = meterRegistry;
        this.redisCacheAvailability = redisCacheAvailability;
        this.cacheErrorHandler = cacheErrorHandler;
        this.singleFlightLocks = Caffeine.newBuilder()
                .maximumSize(MAX_SINGLE_FLIGHT_LOCKS)
                .expireAfterAccess(Duration.ofMinutes(5))
                .build();
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

        ValueWrapper redisValue = getFromRedis(key);
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

        T redisValue = getFromRedis(key, type);
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

        ReentrantLock lock = singleFlightLocks.get(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            ValueWrapper cachedValueAfterLock = get(key);
            if (cachedValueAfterLock != null) {
                return (T) cachedValueAfterLock.get();
            }

            T loadedValue = valueLoader.call();
            put(key, loadedValue);
            return loadedValue;
        } catch (Throwable ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        } finally {
            lock.unlock();
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
        putToRedis(key, value);
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
        evictFromRedis(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean localEvicted = localCache.evictIfPresent(key);
        boolean redisEvicted = evictFromRedisIfPresent(key);
        return localEvicted || redisEvicted;
    }

    @Override
    public void clear() {
        localCache.clear();
        clearRedis();
    }

    @Override
    public boolean invalidate() {
        boolean localInvalidated = localCache.invalidate();
        boolean redisInvalidated = invalidateRedis();
        return localInvalidated || redisInvalidated;
    }

    private ValueWrapper getFromRedis(Object key) {
        if (!canUseRedis("get")) {
            return null;
        }

        try {
            ValueWrapper value = redisCache.get(key);
            redisCacheAvailability.recordSuccess();
            return value;
        } catch (RuntimeException exception) {
            handleRedisGetError(key, exception);
            return null;
        }
    }

    private <T> T getFromRedis(Object key, Class<T> type) {
        if (!canUseRedis("get")) {
            return null;
        }

        try {
            T value = redisCache.get(key, type);
            redisCacheAvailability.recordSuccess();
            return value;
        } catch (RuntimeException exception) {
            handleRedisGetError(key, exception);
            return null;
        }
    }

    private void putToRedis(Object key, Object value) {
        if (!canUseRedis("put")) {
            return;
        }

        try {
            redisCache.put(key, value);
            redisCacheAvailability.recordSuccess();
        } catch (RuntimeException exception) {
            redisCacheAvailability.recordFailure(exception);
            cacheErrorHandler.handleCachePutError(exception, redisCache, key, value);
        }
    }

    private void evictFromRedis(Object key) {
        if (!canUseRedis("evict")) {
            return;
        }

        try {
            redisCache.evict(key);
            redisCacheAvailability.recordSuccess();
        } catch (RuntimeException exception) {
            redisCacheAvailability.recordFailure(exception);
            cacheErrorHandler.handleCacheEvictError(exception, redisCache, key);
        }
    }

    private boolean evictFromRedisIfPresent(Object key) {
        if (!canUseRedis("evict")) {
            return false;
        }

        try {
            boolean evicted = redisCache.evictIfPresent(key);
            redisCacheAvailability.recordSuccess();
            return evicted;
        } catch (RuntimeException exception) {
            redisCacheAvailability.recordFailure(exception);
            cacheErrorHandler.handleCacheEvictError(exception, redisCache, key);
            return false;
        }
    }

    private void clearRedis() {
        if (!canUseRedis("clear")) {
            return;
        }

        try {
            redisCache.clear();
            redisCacheAvailability.recordSuccess();
        } catch (RuntimeException exception) {
            redisCacheAvailability.recordFailure(exception);
            cacheErrorHandler.handleCacheClearError(exception, redisCache);
        }
    }

    private boolean invalidateRedis() {
        if (!canUseRedis("clear")) {
            return false;
        }

        try {
            boolean invalidated = redisCache.invalidate();
            redisCacheAvailability.recordSuccess();
            return invalidated;
        } catch (RuntimeException exception) {
            redisCacheAvailability.recordFailure(exception);
            cacheErrorHandler.handleCacheClearError(exception, redisCache);
            return false;
        }
    }

    private boolean canUseRedis(String operation) {
        if (redisCacheAvailability.canUseRedis()) {
            return true;
        }

        meterRegistry.counter(
                        REDIS_BYPASSED_METRIC,
                        CACHE_TAG, getName(),
                        OPERATION_TAG, operation)
                .increment();
        return false;
    }

    private void handleRedisGetError(Object key, RuntimeException exception) {
        redisCacheAvailability.recordFailure(exception);
        cacheErrorHandler.handleCacheGetError(exception, redisCache, key);
    }

    private void record(String metricName) {
        meterRegistry.counter(metricName, CACHE_TAG, getName())
                .increment();
    }
}
