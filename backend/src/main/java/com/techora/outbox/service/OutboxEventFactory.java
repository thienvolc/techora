package com.techora.outbox.service;

import com.techora.common.infra.config.prop.EventPublisherProperties;
import com.techora.common.infra.service.JsonCodec;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.dto.OutboxPayload;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final EventPublisherProperties eventPublisherProperties;
    private final JsonCodec jsonCodec;
    private final PathPatternRequestMatcher.Builder builder;

    public OutboxEventEntity create(OutboxEventRecord record) {
        Instant now = Instant.now();
        UUID outboxId = UUID.randomUUID();
        Map<String, String> headers = headers(record, outboxId);
        String payload = buildPayload(outboxId, record.eventVersion(), record.attributes());

        return OutboxEventEntity.builder()
                .id(outboxId)
                .eventId(outboxId)
                .aggregateType(record.aggregateType())
                .aggregateId(record.aggregateId())
                .eventType(record.eventType())
                .topic(resolveTopic(record))
                .messageKey(record.messageKey())
                .eventVersion(record.eventVersion())
                .headers(jsonCodec.toJson(headers))
                .payload(payload)
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .nextAttemptAt(now)
                .build();
    }

    public String buildPayload(UUID eventId,
                               int eventVersion,
                               Map<String, Object> attributes) {

        OutboxPayload payload = new OutboxPayload(
                eventId,
                eventVersion,
                Map.copyOf(attributes)
        );
        return jsonCodec.toJson(payload);
    }

    private String resolveTopic(OutboxEventRecord record) {
        return StringUtils.hasText(record.topic())
                ? record.topic()
                : eventPublisherProperties.topic();
    }

    private Map<String, String> headers(OutboxEventRecord record, UUID eventId) {
        Map<String, String> headers = new LinkedHashMap<>(record.headers());
        headers.putIfAbsent("eventId", eventId.toString());
        headers.putIfAbsent("eventType", record.eventType().name());
        headers.putIfAbsent("eventVersion", String.valueOf(record.eventVersion()));
        headers.putIfAbsent("aggregateType", record.aggregateType().name());
        headers.putIfAbsent("aggregateId", record.aggregateId().toString());
        return headers;
    }
}
