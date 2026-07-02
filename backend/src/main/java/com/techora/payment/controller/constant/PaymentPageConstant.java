package com.techora.payment.controller.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

public class PaymentPageConstant {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_SIZE = 100;
    public static final Sort CREATED_AT_DESCENDING = Sort.by(Sort.Direction.DESC, "createdAt");

    private PaymentPageConstant() {
    }
}
