package com.techora.payment.controller.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId
) {
}
