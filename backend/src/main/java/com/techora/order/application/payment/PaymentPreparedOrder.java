package com.techora.order.application.payment;

import com.techora.order.domain.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentPreparedOrder(
        UUID orderId,
        UUID userId,
        String username,
        BigDecimal total,
        Instant paymentWindowExpiresAt
) {
    static PaymentPreparedOrder from(Order order) {
        return new PaymentPreparedOrder(
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getTotal(),
                order.getPaymentDeadlineAt()
        );
    }
}
