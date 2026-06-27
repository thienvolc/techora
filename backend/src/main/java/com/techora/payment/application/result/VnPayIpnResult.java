package com.techora.payment.application.result;

public record VnPayIpnResult(
        String responseCode,
        String message
) {
}
