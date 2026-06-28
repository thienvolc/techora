package com.techora.payment.application.command;

import java.time.Instant;
import java.util.UUID;

public record CreatePaymentCommand(
        UUID userId,
        UUID orderId,
        Instant paymentWindowExpiresAt,
        String idempotencyKey
) {
}
