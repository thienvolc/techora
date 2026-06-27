package com.techora.payment.infra.gateway.vnpay;

import com.techora.payment.infra.config.prop.VnPayProperties;
import com.techora.payment.application.port.gateway.CreateVnPayPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class VnPayParamsBuilder {
    private static final String VERSION = "2.1.0";
    private static final String COMMAND = "pay";
    private static final String CURRENCY = "VND";
    private static final String ORDER_TYPE = "other";
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("100");

    private static final String LOCALE = "vn";
    private static final ZoneId VNPAY_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VNPAY_TIME_ZONE);

    private final VnPayProperties properties;
    private final Clock clock;

    public Map<String, String> build(CreateVnPayPaymentRequest request) {
        Instant createdAt = clock.instant();
        Instant expiredAt = createdAt.plus(Duration.ofMinutes(properties.paymentTimeoutMinutes()));

        Map<String, String> params = new HashMap<>();
        params.put(VnPayParams.VERSION, VERSION);
        params.put(VnPayParams.COMMAND, COMMAND);

        params.put(VnPayParams.TMN_CODE, properties.terminalCode());
        params.put(VnPayParams.AMOUNT, toVnPayAmount(request.amount()));
        params.put(VnPayParams.CURRENCY_CODE, CURRENCY);

        params.put(VnPayParams.TXN_REF, request.txnRef());
        params.put(VnPayParams.RETURN_URL, buildReturnUrl(request.txnRef()));

        params.put(VnPayParams.ORDER_TYPE, ORDER_TYPE);
        params.put(VnPayParams.ORDER_INFO, request.orderInfo());

        params.put(VnPayParams.CREATE_DATE, formatVnPayDate(createdAt));
        params.put(VnPayParams.EXPIRE_DATE, formatVnPayDate(expiredAt));

        params.put(VnPayParams.LOCALE, LOCALE);
        params.put(VnPayParams.IP_ADDRESS, request.ipAddress());

        return params;
    }

    private String buildReturnUrl(String txnRef) {
        String returnUrl = properties.returnUrl();
        return returnUrl.formatted(txnRef);
    }

    private String formatVnPayDate(Instant instant) {
        return VNPAY_DATE_FORMATTER.format(instant);
    }

    private String toVnPayAmount(BigDecimal amount) {
        return amount.multiply(DEFAULT_MULTIPLIER)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
