package com.techora.outbox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techora.common.application.util.StringUtils;
import com.techora.common.infra.service.JsonCodec;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxHeadersCodec {

    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<>() {};
    private final JsonCodec jsonCodec;

    public Map<String, String> deserialize(OutboxEventEntity event) {
        String headers = event.getHeaders();

        if (StringUtils.hasText(headers)) {
            return jsonCodec.fromJson(headers, HEADERS_TYPE);
        }

        return Map.of();
    }
}
