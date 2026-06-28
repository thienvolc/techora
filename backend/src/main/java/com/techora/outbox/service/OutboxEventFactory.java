package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxEventFactory {
    private final OutboxPayloadFactory payloadFactory;

    public OutboxEventEntity create(OutboxEventRecord record) {
        Instant now = Instant.now();
        return OutboxEventEntity.builder()
                .aggregateType(record.aggregateType())
                .aggregateId(record.aggregateId())
                .eventType(record.eventType())
                .payload(payloadFactory.createPayload(record.attributes()))
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .nextAttemptAt(now)
                .build();
    }
}
