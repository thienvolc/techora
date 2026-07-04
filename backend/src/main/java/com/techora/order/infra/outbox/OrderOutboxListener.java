package com.techora.order.infra.outbox;

import com.techora.order.domain.event.OrderPlacedEvent;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOutboxListener {

    private final OrderOutboxRecorder orderOutboxEventPublisher;

    @EventListener
    public void on(OrderPlacedEvent event) {
        orderOutboxEventPublisher.record(event);
    }

    @EventListener
    public void on(OrderStatusChangedEvent event) {
        orderOutboxEventPublisher.record(event);
    }
}
