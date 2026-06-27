package com.techora.payment.application.policy;

import com.techora.common.application.aop.BusinessException;
import com.techora.payment.application.result.VnPayIpnResult;
import org.springframework.stereotype.Service;

@Service
public class VnPayIpnResponsePolicy {
    private static final VnPayIpnResult SUCCESS = new VnPayIpnResult("00", "Successful");
    private static final VnPayIpnResult ORDER_NOT_FOUND = new VnPayIpnResult("01", "Order not found");
    private static final VnPayIpnResult ORDER_ALREADY_CONFIRMED = new VnPayIpnResult("02", "Order already confirmed");
    private static final VnPayIpnResult INVALID_AMOUNT = new VnPayIpnResult("04", "Invalid amount");
    private static final VnPayIpnResult SIGNATURE_FAILED = new VnPayIpnResult("97", "Signature failed");
    private static final VnPayIpnResult UNKNOWN_ERROR = new VnPayIpnResult("99", "Unknown error");

    public VnPayIpnResult signatureFailed() {
        return SIGNATURE_FAILED;
    }

    public VnPayIpnResult unknownError() {
        return UNKNOWN_ERROR;
    }

    public VnPayIpnResult success() {
        return SUCCESS;
    }

    public VnPayIpnResult fromBusinessException(BusinessException ex) {
        return switch (ex.getResponseCode()) {
            case PAYMENT_NOT_FOUND -> ORDER_NOT_FOUND;
            case PAYMENT_AMOUNT_MISMATCH -> INVALID_AMOUNT;
            case INVALID_PAYMENT_STATUS_TRANSITION -> ORDER_ALREADY_CONFIRMED;
            default -> UNKNOWN_ERROR;
        };
    }
}
