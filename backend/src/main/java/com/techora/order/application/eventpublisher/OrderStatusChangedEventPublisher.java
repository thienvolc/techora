package com.techora.order.application.eventpublisher;

import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderStatusChangedEventPublisher {
    private final InternalEventPublisher internalEventPublisher;

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
                        statusChange.order().getUpdatedAt()
                ));
    }
}
