package com.techora.common.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.events")
public record EventPublisherProperties(String mode, String topic, boolean fallbackOnKafkaError) {
    public EventPublisherProperties {
        mode = StringUtils.hasText(mode) ? mode : "local";
        topic = StringUtils.hasText(topic) ? topic : "techora.events";
    }
}
