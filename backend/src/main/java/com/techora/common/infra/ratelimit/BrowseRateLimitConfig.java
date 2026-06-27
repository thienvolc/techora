package com.techora.common.infra.ratelimit;

import com.techora.common.infra.config.prop.BrowseRateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BrowseRateLimitProperties.class)
public class BrowseRateLimitConfig implements WebMvcConfigurer {

    private final BrowseRateLimitInterceptor browseRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(browseRateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/products",
                        "/api/v1/products/*",
                        "/api/v1/categories"
                );
    }
}
