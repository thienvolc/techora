package com.techora.outbox.publisher;

import com.techora.outbox.dto.OutboxMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@ConditionalOnProperty(
        prefix = "app.events",
        name = "mode", havingValue = "local",
        matchIfMissing = true
)
public class LocalOutboxMessagePublisher implements OutboxMessagePublisher {

    @Override
    public CompletableFuture<Void> publish(OutboxMessage message) {
        log.debug(
                "Outbox message published in local mode. eventId={}, eventType={}, topic={}",
                message.eventId(),
                message.eventType(),
                message.topic()
        );
        return CompletableFuture.completedFuture(null);
    }
}
