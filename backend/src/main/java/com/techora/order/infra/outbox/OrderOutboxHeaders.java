package com.techora.order.infra.outbox;

import java.util.Map;

public class OrderOutboxHeaders {

    public static Map<String, String> values() {
        return Map.of(
                "source", "order"
        );
    }
}
