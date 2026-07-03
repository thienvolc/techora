package com.techora.common.infra.config.prop;

import com.techora.outbox.constant.OutboxEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.outbox.relay")
public record OutboxRelayProperties(
        int batchSize,
        List<OutboxEventType> eventTypes
) {
    public OutboxRelayProperties {
        if (batchSize <= 0) batchSize = 100;
        eventTypes = List.of(
                OutboxEventType.PAYMENT_CONFIRMED,
                OutboxEventType.PAYMENT_FAILED,
                OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED
        );
    }
}
