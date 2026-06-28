package com.techora.order.application.mapper;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.command.PlaceOrderItemCommand;
import com.techora.order.application.result.OrderItemResult;
import com.techora.order.application.result.OrderResult;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderItem;
import com.techora.order.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderMapper {

    public OrderResult toResult(Order order) {
        return new OrderResult(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotal(),
                order.getItems().stream().map(this::toItemResult).toList(),
                order.getPaymentDeadlineAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public PageResponse<OrderResult> toPageResult(Page<Order> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResult).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private OrderItemResult toItemResult(OrderItem item) {
        return new OrderItemResult(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    public Order toOrder(PlaceOrderCommand command, Instant createdAt, Instant paymentDeadlineAt) {
        return Order.builder()
                .userId(command.userId())
                .username(command.username())
                .status(OrderStatus.CREATED)
                .total(command.total())
                .paymentDeadlineAt(paymentDeadlineAt)
                .items(command.items().stream()
                        .map(this::toOrderItem)
                        .toList())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private OrderItem toOrderItem(PlaceOrderItemCommand item) {
        return OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName())
                .sku(item.sku())
                .unitPrice(item.unitPrice())
                .quantity(item.quantity())
                .subtotal(item.subtotal())
                .build();
    }
}
