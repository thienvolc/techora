package com.techora.payment.domain.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class PaymentReferenceGenerator {

    private static final String REFERENCE_PREFIX = "pay";

    private PaymentReferenceGenerator() {
    }

    public String generate() {
        return REFERENCE_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
