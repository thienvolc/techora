package com.techora.payment.application.policy;

import com.techora.common.application.aop.BusinessException;
import com.techora.payment.application.model.VnPayIpnReply;
import org.springframework.stereotype.Service;

@Service
public class VnPayIpnReplyPolicy {
    private static final VnPayIpnReply SUCCESS = new VnPayIpnReply("00", "Successful");
    private static final VnPayIpnReply ORDER_NOT_FOUND = new VnPayIpnReply("01", "Order not found");
    private static final VnPayIpnReply ORDER_ALREADY_CONFIRMED = new VnPayIpnReply("02", "Order already confirmed");
    private static final VnPayIpnReply INVALID_AMOUNT = new VnPayIpnReply("04", "Invalid amount");
    private static final VnPayIpnReply SIGNATURE_FAILED = new VnPayIpnReply("97", "Signature failed");
    private static final VnPayIpnReply UNKNOWN_ERROR = new VnPayIpnReply("99", "Unknown error");

    public VnPayIpnReply orderNotFound() {
        return ORDER_NOT_FOUND;
    }

    public VnPayIpnReply invalidAmount() {
        return INVALID_AMOUNT;
    }

    public VnPayIpnReply signatureFailed() {
        return SIGNATURE_FAILED;
    }

    public VnPayIpnReply unknownError() {
        return UNKNOWN_ERROR;
    }

    public VnPayIpnReply success() {
        return SUCCESS;
    }

    public VnPayIpnReply fromBusinessException(BusinessException ex) {
        return switch (ex.getResponseCode()) {
            case PAYMENT_NOT_FOUND -> ORDER_NOT_FOUND;
            case PAYMENT_AMOUNT_MISMATCH -> INVALID_AMOUNT;
            case INVALID_PAYMENT_STATUS_TRANSITION -> ORDER_ALREADY_CONFIRMED;
            default -> UNKNOWN_ERROR;
        };
    }
}
