package com.techora.common.infra.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.techora.common.infra.config.prop.CacheLocalProperties;
import com.techora.common.infra.config.prop.CacheTtlProperties;
import com.techora.common.infra.config.prop.RedisCacheBypassProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties({
        CacheLocalProperties.class,
        CacheTtlProperties.class,
        RedisCacheBypassProperties.class
})
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager,
                                     MeterRegistry meterRegistry,
                                     RedisCacheAvailability redisCacheAvailability,
                                     CacheErrorHandler cacheErrorHandler) {

        return new TwoLevelCacheManager(
                caffeineCacheManager,
                redisCacheManager,
                meterRegistry,
                redisCacheAvailability,
                cacheErrorHandler);
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager(CacheTtlProperties cacheTtlProperties,
                                                     CacheLocalProperties cacheLocalProperties) {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.setCacheNames(CacheNames.all());
        cacheManager.registerCustomCache(
                CacheNames.PRODUCT_DETAIL_BY_SLUG,
                caffeine(cacheTtlProperties.localProductDetail(), cacheLocalProperties.maximumSize()).build());
        cacheManager.registerCustomCache(
                CacheNames.PRODUCT_LISTING,
                caffeine(cacheTtlProperties.localProductListing(), cacheLocalProperties.maximumSize()).build());
        cacheManager.registerCustomCache(
                CacheNames.ACTIVE_CATEGORIES,
                caffeine(cacheTtlProperties.localActiveCategories(), cacheLocalProperties.maximumSize()).build());
        return cacheManager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                               CacheTtlProperties cacheTtlProperties) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(cacheTypeValidator())
                                .build()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(cacheTtlProperties.defaultTtl()))
                .withInitialCacheConfigurations(Map.of(
                        CacheNames.PRODUCT_DETAIL_BY_SLUG, defaultConfig.entryTtl(cacheTtlProperties.productDetail()),
                        CacheNames.PRODUCT_LISTING, defaultConfig.entryTtl(cacheTtlProperties.productListing()),
                        CacheNames.ACTIVE_CATEGORIES, defaultConfig.entryTtl(cacheTtlProperties.activeCategories())
                ))
                .disableCreateOnMissingCache()
                .build();
    }

    private Caffeine<Object, Object> caffeine(java.time.Duration ttl, long maximumSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize)
                .recordStats();
    }

    private BasicPolymorphicTypeValidator cacheTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.techora.")
                .allowIfSubType("java.util.")
                .build();
    }
}
