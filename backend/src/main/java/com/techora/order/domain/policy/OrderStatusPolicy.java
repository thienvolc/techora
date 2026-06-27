package com.techora.order.domain.policy;

import com.techora.order.domain.entity.OrderStatus;

import java.util.Map;
import java.util.Set;

public final class OrderStatusPolicy {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(
                    OrderStatus.STOCK_RESERVED,
                    OrderStatus.CANCELLED),

            OrderStatus.STOCK_RESERVED, Set.of(
                    OrderStatus.PAYMENT_PENDING,
                    OrderStatus.CANCELLED),

            OrderStatus.PAYMENT_PENDING, Set.of(
                    OrderStatus.PAID,
                    OrderStatus.PAYMENT_FAILED),

            OrderStatus.PAID, Set.of(
                    OrderStatus.FULFILLING,
                    OrderStatus.CANCELLED),

            OrderStatus.PAYMENT_FAILED, Set.of(
                    OrderStatus.CANCELLED),

            OrderStatus.CANCELLED, Set.of(),

            OrderStatus.FULFILLING, Set.of(
                    OrderStatus.SHIPPED,
                    OrderStatus.CANCELLED),

            OrderStatus.SHIPPED, Set.of(
                    OrderStatus.DELIVERED),

            OrderStatus.DELIVERED, Set.of()
    );

    private OrderStatusPolicy() {
    }

    public static boolean canTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(nextStatus);
    }
}
