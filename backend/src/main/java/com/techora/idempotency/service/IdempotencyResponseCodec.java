package com.techora.idempotency.service;

import com.techora.common.infra.service.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyResponseCodec {
    private final JsonCodec jsonCodec;

    public String serialize(Object response) {
        return jsonCodec.toJson(response);
    }

    public <T> T deserialize(String payload, Class<T> responseType) {
        return jsonCodec.fromJson(payload, responseType);
    }
}
