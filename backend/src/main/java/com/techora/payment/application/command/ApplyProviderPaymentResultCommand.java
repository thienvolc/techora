package com.techora.payment.application.command;

import com.techora.payment.application.service.PaymentProvider;

import java.math.BigDecimal;

public record ApplyProviderPaymentResultCommand(
        String providerReference,
        BigDecimal amount,
        boolean successful,
        PaymentProvider providerName
) {
}
