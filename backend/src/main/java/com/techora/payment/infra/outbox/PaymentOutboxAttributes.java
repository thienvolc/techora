package com.techora.payment.infra.outbox;

import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;

import java.util.Map;

public class PaymentOutboxAttributes {

    public static final String EVENT_VERSION = "eventVersion";
    public static final String PAYMENT_ID = "paymentId";
    public static final String ATTEMPT_ID = "attemptId";
    public static final String ORDER_ID = "orderId";
    public static final String USER_ID = "userId";
    public static final String AMOUNT = "amount";
    public static final String PROVIDER_NAME = "providerName";
    public static final String PROVIDER_REFERENCE = "providerReference";
    public static final String REASON = "reason";
    public static final String OCCURRED_AT = "occurredAt";

    private PaymentOutboxAttributes() {
    }

    public static Map<String, Object> confirmed(PaymentConfirmedEvent event) {
        return Map.ofEntries(
                Map.entry(EVENT_VERSION, event.eventVersion()),
                Map.entry(PAYMENT_ID, event.paymentId()),
                Map.entry(ATTEMPT_ID, event.attemptId()),
                Map.entry(ORDER_ID, event.orderId()),
                Map.entry(USER_ID, event.userId()),
                Map.entry(PROVIDER_NAME, event.providerName().name()),
                Map.entry(PROVIDER_REFERENCE, event.providerReference()),
                Map.entry(AMOUNT, event.amount()),
                Map.entry(OCCURRED_AT, event.occurredAt())
        );
    }

    public static Map<String, Object> failed(PaymentFailedEvent event) {
        return Map.ofEntries(
                Map.entry(EVENT_VERSION, event.eventVersion()),
                Map.entry(PAYMENT_ID, event.paymentId()),
                Map.entry(ATTEMPT_ID, event.attemptId()),
                Map.entry(ORDER_ID, event.orderId()),
                Map.entry(USER_ID, event.userId()),
                Map.entry(PROVIDER_NAME, event.providerName().name()),
                Map.entry(PROVIDER_REFERENCE, event.providerReference()),
                Map.entry(AMOUNT, event.amount()),
                Map.entry(OCCURRED_AT, event.occurredAt())
        );
    }

    public static Map<String, Object> reconciliationRequired(PaymentReconciliationRequiredEvent event) {
        return Map.ofEntries(
                Map.entry(EVENT_VERSION, event.eventVersion()),
                Map.entry(PAYMENT_ID, event.paymentId()),
                Map.entry(ATTEMPT_ID, event.attemptId()),
                Map.entry(ORDER_ID, event.orderId()),
                Map.entry(USER_ID, event.userId()),
                Map.entry(PROVIDER_NAME, event.providerName().name()),
                Map.entry(PROVIDER_REFERENCE, event.providerReference()),
                Map.entry(REASON, event.reason().name()),
                Map.entry(AMOUNT, event.amount()),
                Map.entry(OCCURRED_AT, event.occurredAt())
        );
    }
}
