package com.techora.order.application.service.payment;

import com.techora.order.domain.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayableOrderSnapshot(
        UUID orderId,
        UUID userId,
        String username,
        BigDecimal total,
        Instant paymentDeadlineAt
) {
    static PayableOrderSnapshot from(Order order) {
        return new PayableOrderSnapshot(
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getTotal(),
                order.getPaymentDeadlineAt()
        );
    }
}
