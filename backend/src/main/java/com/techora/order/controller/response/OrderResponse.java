package com.techora.order.controller.response;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.order.application.result.OrderResult;
import com.techora.order.domain.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemResponse> items,
        Instant paymentDeadlineAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(OrderResult result) {
        return new OrderResponse(
                result.id(),
                result.userId(),
                result.status(),
                result.total(),
                result.items().stream().map(OrderItemResponse::from).toList(),
                result.paymentDeadlineAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static PageResponse<OrderResponse> from(PageResponse<OrderResult> result) {
        return new PageResponse<>(
                result.items().stream().map(OrderResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages()
        );
    }
}
