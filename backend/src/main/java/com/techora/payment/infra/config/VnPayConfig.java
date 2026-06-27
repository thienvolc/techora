package com.techora.payment.infra.config;

import com.techora.payment.infra.config.prop.VnPayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(VnPayProperties.class)
public class VnPayConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
