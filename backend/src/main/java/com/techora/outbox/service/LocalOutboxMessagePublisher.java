package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.port.OutboxMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(
        prefix = "app.events",
        name = "mode", havingValue = "local",
        matchIfMissing = true
)
public class LocalOutboxMessagePublisher implements OutboxMessagePublisher {

    @Override
    public void publish(OutboxMessage message) {
        log.debug(
                "Outbox message published in local mode. eventId={}, eventType={}, topic={}",
                message.eventId(),
                message.eventType(),
                message.topic()
        );
    }
}
