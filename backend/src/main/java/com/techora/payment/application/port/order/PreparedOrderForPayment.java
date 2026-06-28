package com.techora.payment.application.port.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PreparedOrderForPayment(
        UUID orderId,
        UUID userId,
        String username,
        BigDecimal total,
        Instant paymentWindowExpiresAt
) {
}
