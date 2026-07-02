package com.techora.order.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.order.application.mapper.OrderMapper;
import com.techora.order.application.model.OrderView;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public PageResponse<OrderView> getUserOrders(UUID userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orderMapper.toPageView(orders);
    }

    @Transactional(readOnly = true)
    public OrderView getUserOrder(UUID userId, UUID orderId) {
        return orderMapper.toView(
                getUserOrderOrThrow(userId, orderId));
    }

    @Transactional(readOnly = true)
    public OrderView getAdminOrder(UUID orderId) {
        return orderMapper.toView(
                getOrderOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countOrdersByStatus() {
        return orderRepository.countOrdersByStatus();
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    private Order getUserOrderOrThrow(UUID userId, UUID orderId) {
        return orderRepository.findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }
}
