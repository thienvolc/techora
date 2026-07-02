package com.techora.payment.application.port.gateway;

import java.util.Map;

public interface VnPayGatewayPort {

    String buildPaymentUrl(CreateVnPayPaymentRequest request);

    VerifiedVnPayIpn verifyAndParseIpn(Map<String, String> params);
}
