package com.techora.outbox.publisher;

import com.techora.outbox.dto.OutboxMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.events",
        name = "mode", havingValue = "kafka"
)
public class KafkaMessagePublisher implements OutboxMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public CompletableFuture<Void> publish(OutboxMessage message) {
        try {
            return kafkaTemplate.send(toProducerRecord(message))
                    .thenApply(result -> null);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private ProducerRecord<String, String> toProducerRecord(OutboxMessage message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                message.topic(),
                message.messageKey(),
                message.payload()
        );
        message.headers().forEach((key, value) -> record.headers()
                .add(key, value.getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
