package com.techora.payment.application.command;

import lombok.Builder;

import java.util.UUID;

@Builder
public record InitiateVnPayPaymentCommand(
        UUID userId,
        UUID orderId,
        String ipAddress,
        String idempotencyKey
) {
}
