package com.techora.checkout.application.port.payment;

import java.time.Instant;
import java.util.UUID;

public record CheckoutPaymentCommand(
        UUID userId,
        UUID orderId,
        Instant paymentWindowExpiresAt,
        String ipAddress
) {
}
