package com.techora.outbox.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class OutboxHeaders {

    private final Map<String, String> values;

    private OutboxHeaders(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static OutboxHeaders of(Map<String, String> values) {
        return new OutboxHeaders(values);
    }

    public OutboxHeaders with(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new OutboxHeaders(copy);
    }

    public Map<String, String> values() {
        return values;
    }
}
