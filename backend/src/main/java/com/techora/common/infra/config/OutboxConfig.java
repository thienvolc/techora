package com.techora.common.infra.config;

import com.techora.common.infra.config.prop.OutboxRelayProperties;
import com.techora.common.infra.config.prop.OutboxRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OutboxRetryProperties.class, OutboxRelayProperties.class})
public class OutboxConfig {
}
