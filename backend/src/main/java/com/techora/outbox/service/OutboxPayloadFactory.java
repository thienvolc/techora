package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxPayload;
import com.techora.common.infra.service.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPayloadFactory {

    private final JsonCodec jsonCodec;

    public String createPayload(Map<String, Object> attributes) {
        return jsonCodec.toJson(new OutboxPayload(UUID.randomUUID(), Map.copyOf(attributes)));
    }
}
