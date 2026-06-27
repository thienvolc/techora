package com.techora.common.infra.config;

import com.techora.common.infra.config.prop.MessagePolicyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessagePolicyProperties.class)
public class MessagePolicyConfig {
}
