package com.techora.payment.infra.gateway.vnpay;

import com.techora.payment.infra.config.prop.VnPayProperties;
import com.techora.common.infra.service.CryptoService;
import com.techora.payment.application.exception.InvalidVnPayPayloadException;
import com.techora.payment.application.exception.InvalidVnPaySignatureException;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.port.gateway.CreateVnPayPaymentRequest;
import com.techora.payment.application.port.gateway.VnPayPaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VnPayGateway implements VnPayGatewayPort {
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("100");

    private final CryptoService cryptoService;
    private final VnPayParamsBuilder paramsBuilder;
    private final VnPayProperties properties;

    @Override
    public String buildPaymentUrl(CreateVnPayPaymentRequest request) {
        Map<String, String> params = paramsBuilder.build(request);
        return buildPaymentUrl(params);
    }

    @Override
    public VnPayPaymentResult verifyAndParseIpn(Map<String, String> params) {
        if (hasInvalidSignature(params)) {
            throw new InvalidVnPaySignatureException();
        }
        try {
            return parseResult(params);
        } catch (RuntimeException ex) {
            throw new InvalidVnPayPayloadException();
        }
    }

    private boolean hasInvalidSignature(Map<String, String> params) {
        String requestSecureHash = params.get(VnPayParams.SECURE_HASH);
        if (!StringUtils.hasText(requestSecureHash)) {
            return true;
        }

        Map<String, String> signedParams = new HashMap<>(params);
        signedParams.remove(VnPayParams.SECURE_HASH);
        signedParams.remove(VnPayParams.SECURE_HASH_TYPE);

        List<String> sortedKeys = sortedKeys(signedParams);
        String secureHash = buildSecureHash(signedParams, sortedKeys);

        return !requestSecureHash.equals(secureHash);
    }

    private VnPayPaymentResult parseResult(Map<String, String> params) {
        String txnRef = params.get(VnPayParams.TXN_REF);
        String amount = params.get(VnPayParams.AMOUNT);
        String responseCode = params.get(VnPayParams.RESPONSE_CODE);
        String transactionStatus = params.get(VnPayParams.TRANSACTION_STATUS);

        return VnPayPaymentResult.builder()
                .txnRef(txnRef)
                .amount(fromVnPayAmount(amount))
                .responseCode(responseCode)
                .transactionStatus(transactionStatus)
                .build();
    }

    private String buildPaymentUrl(Map<String, String> params) {
        List<String> sortedKeys = sortedKeys(params);

        String queryString = buildQueryString(params, sortedKeys);
        String secureHash = buildSecureHash(params, sortedKeys);

        return properties.payUrl()
                + Symbol.QUERY_SEPARATOR + queryString
                + Symbol.AND + VnPayParams.SECURE_HASH + Symbol.EQUAL + secureHash;
    }

    private String buildSecureHash(Map<String, String> params, List<String> sortedKeys) {
        String hashPayload = sortedKeys.stream()
                .map(key -> key + Symbol.EQUAL + encodeUrl(params.get(key)))
                .collect(Collectors.joining(Symbol.AND));

        return cryptoService.hmacSha512(properties.hashSecret(), hashPayload);
    }

    private String buildQueryString(Map<String, String> params, List<String> sortedKeys) {
        return sortedKeys.stream()
                .map(key -> encodeUrl(key) + Symbol.EQUAL + encodeUrl(params.get(key)))
                .collect(Collectors.joining(Symbol.AND));
    }

    private List<String> sortedKeys(Map<String, String> params) {
        return params.keySet().stream()
                .sorted()
                .toList();
    }

    private BigDecimal fromVnPayAmount(String amount) {
        return new BigDecimal(amount)
                .divide(DEFAULT_MULTIPLIER, 2, RoundingMode.UNNECESSARY);
    }

    private String encodeUrl(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
