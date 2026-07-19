package com.techora.payment;

import com.techora.common.infra.service.CryptoService;
import com.techora.payment.infra.gateway.vnpay.VnPayParams;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class VnPayIpnTestParams {
    private static final String VERSION = "2.1.0";
    private static final String COMMAND = "pay";
    private static final String CURRENCY = "VND";
    private static final String ORDER_TYPE = "other";
    private static final String LOCALE = "vn";
    private static final String TMN_CODE = "DEMO";

    private static final BigDecimal VNPAY_AMOUNT_MULTIPLIER = new BigDecimal("100");
    private static final String HASH_SECRET = "DEMO";

    private VnPayIpnTestParams() {
    }

    static Map<String, String> successful(String txnRef, BigDecimal amount) {
        Map<String, String> params = baseParams(txnRef, amount);
        params.put(VnPayParams.RESPONSE_CODE, "00");
        params.put(VnPayParams.TRANSACTION_STATUS, "00");
        return signed(params);
    }

    static Map<String, String> failed(String txnRef, BigDecimal amount) {
        Map<String, String> params = baseParams(txnRef, amount);
        params.put(VnPayParams.RESPONSE_CODE, "24");
        params.put(VnPayParams.TRANSACTION_STATUS, "02");
        return signed(params);
    }

    static Map<String, String> successfulWithInvalidSignature(String txnRef, BigDecimal amount) {
        Map<String, String> params = successful(txnRef, amount);
        params.put(VnPayParams.SECURE_HASH, "invalid-signature");
        return params;
    }

    private static Map<String, String> baseParams(String txnRef, BigDecimal amount) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(VnPayParams.VERSION, VERSION);
        params.put(VnPayParams.COMMAND, COMMAND);

        params.put(VnPayParams.TMN_CODE, TMN_CODE);
        params.put(VnPayParams.AMOUNT, toVnPayAmount(amount));
        params.put(VnPayParams.CURRENCY_CODE, CURRENCY);

        params.put(VnPayParams.TXN_REF, txnRef);

        params.put(VnPayParams.ORDER_TYPE, ORDER_TYPE);
        params.put(VnPayParams.ORDER_INFO, "Payment " + txnRef);

        params.put(VnPayParams.CREATE_DATE, "20260708190000");

        params.put(VnPayParams.LOCALE, LOCALE);

        params.put(VnPayParams.TRANSACTION_NO, "14123456");
        return params;
    }

    private static Map<String, String> signed(Map<String, String> params) {
        Map<String, String> signedParams = new LinkedHashMap<>(params);
        signedParams.put(VnPayParams.SECURE_HASH, secureHash(params));
        return signedParams;
    }

    private static String secureHash(Map<String, String> params) {
        String payload = params.keySet().stream()
                .sorted()
                .map(key -> key + "=" + encode(params.get(key)))
                .collect(Collectors.joining("&"));

        return new CryptoService().hmacSha512(HASH_SECRET, payload);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String toVnPayAmount(BigDecimal amount) {
        return amount.multiply(VNPAY_AMOUNT_MULTIPLIER)
                .toBigIntegerExact()
                .toString();
    }
}
