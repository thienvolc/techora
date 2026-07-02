package com.techora.payment.application.model;

public record VnPayIpnReply(
        String responseCode,
        String message
) {
}
