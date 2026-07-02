package com.techora.order.application.port.payment;

import java.time.Instant;
import java.util.UUID;

public record InitiateOrderPaymentCommand(
        UUID userId,
        UUID orderId,
        Instant paymentDeadlineAt,
        String ipAddress,
        String idempotencyKey
) {
}
