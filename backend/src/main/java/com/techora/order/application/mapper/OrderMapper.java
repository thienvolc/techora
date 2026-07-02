package com.techora.order.application.mapper;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.order.application.model.OrderItemView;
import com.techora.order.application.model.OrderView;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public PageResponse<OrderView> toPageView(Page<Order> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toView).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public OrderView toView(Order order) {
        return new OrderView(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotal(),
                order.getItems().stream().map(this::toItemView).toList(),
                order.getPaymentDeadlineAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemView toItemView(OrderItem item) {
        return new OrderItemView(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
