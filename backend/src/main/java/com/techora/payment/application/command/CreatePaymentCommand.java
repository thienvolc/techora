package com.techora.payment.application.command;

import com.techora.payment.domain.valueobject.PaymentProvider;

import java.util.UUID;

public record CreatePaymentCommand(
        UUID userId,
        UUID orderId,
        PaymentProvider provider
) {
}
