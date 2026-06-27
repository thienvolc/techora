package com.techora.order.application.service;

import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.mapper.OrderMapper;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.application.result.OrderSnapshot;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderPlacementService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InternalEventPublisher internalEventPublisher;
    private final Clock clock;

    @Transactional
    public OrderSnapshot place(PlaceOrderCommand command) {
        Order order = createOrder(command);
        publishOrderPlacedEvent(order);
        return OrderSnapshot.from(order);
    }

    private Order createOrder(PlaceOrderCommand command) {
        Order order = orderMapper.toOrder(command, now());
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
