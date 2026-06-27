package com.techora.payment.application.command;

import java.util.UUID;

public record CreatePaymentCommand(
        UUID userId,
        UUID orderId,
        String idempotencyKey
) {
}
