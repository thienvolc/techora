package com.techora.inventory.infra.outbox;

import java.util.Map;

public class InventoryOutboxHeaders {

    public static Map<String, String> values() {
        return Map.of(
                "source", "inventory"
        );
    }
}
