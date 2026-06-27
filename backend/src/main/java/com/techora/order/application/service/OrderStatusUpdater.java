package com.techora.order.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.application.eventpublisher.OrderStatusChangedEventPublisher;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderStatusUpdater {
    private final OrderRepository orderRepository;
    private final OrderStatusChangedEventPublisher statusChangedEventPublisher;

    @Transactional
    public OrderStatusChange update(UpdateOrderStatusCommand command) {
        OrderStatusChange statusChange = updateStatus(command);
        statusChangedEventPublisher.publish(statusChange);
        return statusChange;
    }

    private OrderStatusChange updateStatus(UpdateOrderStatusCommand command) {
        Order order = getOrder(command.orderId());

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = command.nextStatus();

        order.changeStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return new OrderStatusChange(
                updatedOrder,
                oldStatus,
                command.nextStatus(),
                command.actor()
        );
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findLockedWithItemsById(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }
}
