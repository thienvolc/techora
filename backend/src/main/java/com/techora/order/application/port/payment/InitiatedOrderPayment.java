package com.techora.order.application.port.payment;

import java.time.Instant;
import java.util.UUID;

public record InitiatedOrderPayment(
        UUID paymentId,
        String paymentUrl,
        Instant expiresAt
) {
}
