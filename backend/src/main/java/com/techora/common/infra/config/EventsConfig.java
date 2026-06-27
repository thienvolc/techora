package com.techora.common.infra.config;

import com.techora.common.infra.config.prop.EventPublisherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventPublisherProperties.class)
public class EventsConfig {
}
