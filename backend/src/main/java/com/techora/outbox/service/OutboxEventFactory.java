package com.techora.outbox.service;

import com.techora.common.application.util.StringUtils;
import com.techora.common.infra.config.prop.EventPublisherProperties;
import com.techora.common.infra.service.JsonCodec;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.dto.IntegrationEventEnvelope;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final EventPublisherProperties eventPublisherProperties;
    private final JsonCodec jsonCodec;

    public OutboxEventEntity create(OutboxEventRecord<?> record) {
        UUID eventId = UUID.randomUUID();
        IntegrationEventEnvelope<?> payload = IntegrationEventEnvelope.from(eventId, record);
        Map<String, String> headers = buildEventHeaders(eventId, record);

        String topic = StringUtils.getOrDefault(record.topic(), eventPublisherProperties.topic());
        Instant now = Instant.now();

        return OutboxEventEntity.builder()
                .id(eventId)
                .eventId(eventId)
                .aggregateType(record.aggregateType())
                .aggregateId(record.aggregateId())
                .eventType(record.eventType())
                .topic(topic)
                .messageKey(record.messageKey())
                .eventVersion(record.eventVersion())
                .headers(jsonCodec.toJson(headers))
                .payload(jsonCodec.toJson(payload))
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .nextAttemptAt(now)
                .build();
    }

    private Map<String, String> buildEventHeaders(UUID eventId, OutboxEventRecord<?> record) {
        return record.headers()
                .with("eventId", eventId.toString())
                .with("eventType", record.eventType().name())
                .with("eventVersion", String.valueOf(record.eventVersion()))
                .values();
    }
}
