package com.techora.common.infra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JsonCodec {

    private final ObjectMapper objectMapper;

    public <T> String toJson(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize value as JSON", ex);
        }
    }

    public <T> T fromJson(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize JSON value", ex);
        }
    }
}
