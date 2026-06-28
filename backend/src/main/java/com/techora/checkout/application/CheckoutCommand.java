package com.techora.checkout.application;

import java.util.UUID;

public record CheckoutCommand(
        UUID userId,
        String ipAddress,
        String idempotencyKey
) {
}
