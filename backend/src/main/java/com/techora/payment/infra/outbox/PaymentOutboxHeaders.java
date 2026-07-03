package com.techora.payment.infra.outbox;

import java.util.Map;

public class PaymentOutboxHeaders {
    public static Map<String, String> values() {
        return Map.of(
                "source", "payment"
        );
    }
}
