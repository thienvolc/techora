package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache.ttl")
public record CacheTtlProperties(
        Duration defaultTtl,
        Duration productDetail,
        Duration productListing,
        Duration activeCategories,
        Duration localProductDetail,
        Duration localProductListing,
        Duration localActiveCategories
) {

    public CacheTtlProperties {
        defaultTtl = positiveOrDefault(defaultTtl, Duration.ofMinutes(10));
        productDetail = positiveOrDefault(productDetail, Duration.ofMinutes(10));
        productListing = positiveOrDefault(productListing, Duration.ofSeconds(60));
        activeCategories = positiveOrDefault(activeCategories, Duration.ofMinutes(30));
        localProductDetail = positiveOrDefault(localProductDetail, Duration.ofSeconds(60));
        localProductListing = positiveOrDefault(localProductListing, Duration.ofSeconds(30));
        localActiveCategories = positiveOrDefault(localActiveCategories, Duration.ofMinutes(5));
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative()
                ? defaultValue
                : value;
    }
}
