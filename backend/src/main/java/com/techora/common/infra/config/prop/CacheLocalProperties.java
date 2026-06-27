package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.local")
public record CacheLocalProperties(long maximumSize) {

    public CacheLocalProperties {
        if (maximumSize <= 0) {
            maximumSize = 10_000;
        }
    }
}
