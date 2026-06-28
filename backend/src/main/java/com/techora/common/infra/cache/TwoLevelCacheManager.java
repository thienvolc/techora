package com.techora.common.infra.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager redisCacheManager;
    private final MeterRegistry meterRegistry;
    private final RedisCacheAvailability redisCacheAvailability;
    private final CacheErrorHandler cacheErrorHandler;
    private final Set<String> cacheNames;

    public TwoLevelCacheManager(CacheManager localCacheManager,
                                CacheManager redisCacheManager,
                                MeterRegistry meterRegistry,
                                RedisCacheAvailability redisCacheAvailability,
                                CacheErrorHandler cacheErrorHandler) {

        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.meterRegistry = meterRegistry;
        this.redisCacheAvailability = redisCacheAvailability;
        this.cacheErrorHandler = cacheErrorHandler;
        this.cacheNames = new LinkedHashSet<>();
        this.cacheNames.addAll(localCacheManager.getCacheNames());
        this.cacheNames.addAll(redisCacheManager.getCacheNames());
    }

    @Override
    public Cache getCache(String name) {
        Cache localCache = localCacheManager.getCache(name);
        Cache redisCache = redisCacheManager.getCache(name);
        if (localCache == null || redisCache == null) {
            return redisCache != null ? redisCache : localCache;
        }
        return new TwoLevelCache(
                localCache,
                redisCache,
                meterRegistry,
                redisCacheAvailability,
                cacheErrorHandler);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheNames;
    }

    @Override
    public void resetCaches() {
        localCacheManager.resetCaches();
        redisCacheManager.resetCaches();
    }
}
