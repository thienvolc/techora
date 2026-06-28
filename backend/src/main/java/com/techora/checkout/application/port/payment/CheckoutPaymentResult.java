package com.techora.checkout.application.port.payment;

import com.techora.order.domain.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record CheckoutPaymentResult(
        UUID paymentId,
        OrderStatus orderStatus,
        String paymentUrl,
        Instant expiresAt
) {
}
