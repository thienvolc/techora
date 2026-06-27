package com.techora.common.infra.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheEvictionService {
    private final CacheManager cacheManager;
    private final CacheErrorHandler cacheErrorHandler;

    public void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null || key == null) {
            return;
        }

        try {
            cache.evictIfPresent(key);
        } catch (RuntimeException exception) {
            cacheErrorHandler.handleCacheEvictError(exception, cache, key);
        }
    }

    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }

        try {
            cache.clear();
        } catch (RuntimeException exception) {
            cacheErrorHandler.handleCacheClearError(exception, cache);
        }
    }
}
