package com.techora.order.application.port.payment;

import java.util.UUID;

public interface PaymentWindowExpirationPort {
    PaymentWindowExpirationResult expirePaymentWindow(UUID orderId);
}
