package com.techora.order.application.eventpublisher;

import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderStatusChangedEventPublisher {
    private final InternalEventPublisher internalEventPublisher;
    private final Clock clock;

    public void publish(OrderStatusChange statusChange) {
        internalEventPublisher.publish(
                new OrderStatusChangedEvent(
                        statusChange.order().getId(),
                        statusChange.order().getUserId(),
                        statusChange.order().getUsername(),
                        statusChange.oldStatus(),
                        statusChange.newStatus(),
                        statusChange.actor().actorId(),
                        statusChange.actor().actorName(),
                        statusChange.actor().actorType(),
                        Instant.now(clock)
                ));
    }
}
