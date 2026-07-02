package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.port.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.events",
        name = "mode", havingValue = "kafka"
)
public class KafkaMessagePublisher implements OutboxMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publish(OutboxMessage message) {
        try {
            kafkaTemplate.send(toProducerRecord(message)).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox message", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Unable to publish outbox message", ex);
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
