package com.techora.payment.application.port.gateway;

import com.techora.payment.application.command.ApplyProviderPaymentResultCommand;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record VnPayPaymentResult(
        String txnRef,
        BigDecimal amount,
        String responseCode,
        String transactionStatus
) {
    private static final String SUCCESS_CODE = "00";

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(responseCode) && SUCCESS_CODE.equals(transactionStatus);
    }
}
