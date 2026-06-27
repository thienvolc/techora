package com.techora.order.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.order.domain.policy.OrderStatusPolicy;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {
    private final UUID id;
    private final UUID userId;
    private final String username;
    private final BigDecimal total;
    private final Instant createdAt;
    private final List<OrderItem> items;

    private OrderStatus status;
    private Instant updatedAt;

    @Builder
    private Order(UUID id,
                  UUID userId,
                  String username,
                  OrderStatus status,
                  BigDecimal total,
                  List<OrderItem> items,
                  Instant createdAt,
                  Instant updatedAt) {

        this.id = id;
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.total = total;
        this.items = new ArrayList<>(items == null ? List.of() : items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeStatus(OrderStatus nextStatus) {
        validateTransition(nextStatus);
        status = nextStatus;
        markUpdated();
    }

    private void validateTransition(OrderStatus nextStatus) {
        if (!OrderStatusPolicy.canTransition(status, nextStatus)) {
            throw new BusinessException(ResponseCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }

    public void markUpdated() {
        updatedAt = Instant.now();
    }
}
