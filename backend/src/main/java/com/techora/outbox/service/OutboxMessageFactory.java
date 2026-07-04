package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxMessageFactory {

    private final OutboxHeadersCodec outboxHeadersCodec;

    public OutboxMessage toMessage(OutboxEventEntity event) {
        Map<String, String> headers = outboxHeadersCodec.deserialize(event);

        return new OutboxMessage(
                event.getEventId(),
                event.getTopic(),
                event.getMessageKey(),
                headers,
                event.getPayload()
        );
    }
}
