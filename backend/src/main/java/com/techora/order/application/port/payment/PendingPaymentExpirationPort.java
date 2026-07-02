package com.techora.order.application.port.payment;

import java.util.UUID;

public interface PendingPaymentExpirationPort {
    PendingPaymentExpirationResult expirePendingPaymentForOrderTimeout(UUID orderId);
}
