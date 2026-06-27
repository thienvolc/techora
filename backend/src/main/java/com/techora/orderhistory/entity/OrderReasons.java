package com.techora.orderhistory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderReasons {
    CHECKOUT_COMPLETED("checkout_completed"),
    USER_PAYMENT_UPDATE("user_payment_update"),
    ADMIN_STATUS_UPDATE("admin_status_update"),
    SYSTEM_STATUS_UPDATE("system_status_update");

    private final String value;
}
