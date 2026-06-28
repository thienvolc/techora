package com.techora.payment.application.port.gateway;

import com.techora.payment.application.result.PaymentResult;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateVnPayPaymentRequest(
        BigDecimal amount,
        String txnRef,
        String orderInfo,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt
) {

    public static CreateVnPayPaymentRequest from(
            PaymentResult paymentResult,
            String ipAddress,
            String orderInfo) {

        return new CreateVnPayPaymentRequest(
                paymentResult.amount(),
                paymentResult.providerReference(),
                orderInfo,
                ipAddress,
                paymentResult.createdAt(),
                paymentResult.expiresAt()
        );
    }
}
