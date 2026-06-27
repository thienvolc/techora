package com.techora.common.infra.cache;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CacheErrorHandlingConfig implements CachingConfigurer {

    private final MeterRegistry meterRegistry;

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler(meterRegistry);
    }
}
