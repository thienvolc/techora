package com.techora.order.application.service;

import com.techora.common.application.policy.OrderPaymentWindowsPolicy;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.order.application.command.CreateOrderCommand;
import com.techora.order.application.mapper.PlaceOrderMapper;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderCreator {
    private final OrderRepository orderRepository;
    private final PlaceOrderMapper placeOrderMapper;
    private final InternalEventPublisher internalEventPublisher;
    private final OrderPaymentWindowsPolicy orderPaymentWindowsPolicy;
    private final Clock clock;

    @Transactional
    public Order create(CreateOrderCommand command) {
        Order order = createOrder(command);
        publishOrderPlacedEvent(order);
        return order;
    }

    private Order createOrder(CreateOrderCommand command) {
        Instant createdAt = now();
        Order order = placeOrderMapper.toOrder(
                command,
                createdAt,
                orderPaymentWindowsPolicy.expiresAt(createdAt)
        );
        return orderRepository.save(order);
    }

    private void publishOrderPlacedEvent(Order order) {
        internalEventPublisher.publish(new OrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                order.getUsername(),
                order.getStatus(),
                order.getTotal(),
                Instant.now(clock)
        ));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
