package com.techora.payment.infra.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.providers.vnpay")
public record VnPayProperties(
        String terminalCode,
        String hashSecret,
        String payUrl,
        String returnUrl,
        int paymentTimeoutMinutes
) {
    public VnPayProperties {
        payUrl = (payUrl == null || payUrl.isBlank())
                ? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                : payUrl;
        paymentTimeoutMinutes = paymentTimeoutMinutes <= 0 ? 15 : paymentTimeoutMinutes;
    }
}
