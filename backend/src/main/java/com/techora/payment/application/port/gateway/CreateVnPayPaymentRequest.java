package com.techora.payment.application.port.gateway;

import com.techora.payment.application.model.PaymentDetails;

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
            PaymentDetails payment,
            String ipAddress,
            String orderInfo) {

        return new CreateVnPayPaymentRequest(
                payment.amount(),
                payment.providerReference(),
                orderInfo,
                ipAddress,
                payment.createdAt(),
                payment.expiresAt()
        );
    }
}
