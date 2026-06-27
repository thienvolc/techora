package com.techora.payment.controller.response;

import com.techora.payment.application.result.InitiateVnPayPaymentResult;

import java.util.UUID;

public record VnPayInitiatePaymentResponse(
        UUID paymentId,
        String paymentUrl
) {

    public static VnPayInitiatePaymentResponse from(InitiateVnPayPaymentResult result) {
        return new VnPayInitiatePaymentResponse(result.paymentId(), result.paymentUrl());
    }
}
