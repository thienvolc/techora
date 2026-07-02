package com.techora.order.application.command;

import java.util.UUID;

public record PlaceOrderCommand(
        UUID userId,
        String ipAddress,
        String idempotencyKey
) {
}
