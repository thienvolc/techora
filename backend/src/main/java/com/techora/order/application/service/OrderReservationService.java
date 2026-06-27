package com.techora.order.application.service;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.application.eventpublisher.OrderStatusChangedEventPublisher;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.application.result.OrderSnapshot;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderReservationService {
    private final OrderStatusChangedEventPublisher statusChangedEventPublisher;
    private final OrderStatusUpdater orderStatusUpdater;

    @Transactional
    public OrderSnapshot markStockReserved(UUID orderId) {
        OrderStatusChange statusChange =
                orderStatusUpdater.update(
                        new UpdateOrderStatusCommand(
                                orderId,
                                OrderStatus.STOCK_RESERVED,
                                OrderActor.system()
                        )
                );
        statusChangedEventPublisher.publish(statusChange);
        return OrderSnapshot.from(statusChange.order());
    }
}
