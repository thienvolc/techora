package com.techora.payment.controller.response;

import com.techora.payment.application.result.VnPayReturnResult;
import com.techora.payment.domain.valueobject.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VnPayReturnResponse(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant expiresAt,
        String message
) {
    public static VnPayReturnResponse from(VnPayReturnResult result) {
        return new VnPayReturnResponse(
                result.paymentId(),
                result.orderId(),
                result.amount(),
                result.status(),
                result.expiresAt(),
                result.message()
        );
    }
}
