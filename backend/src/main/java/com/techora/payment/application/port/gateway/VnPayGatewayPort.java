package com.techora.payment.application.port.gateway;

import java.util.Map;

public interface VnPayGatewayPort {

    String buildPaymentUrl(CreateVnPayPaymentRequest request);

    VnPayPaymentResult verifyAndParseIpn(Map<String, String> params);
}
