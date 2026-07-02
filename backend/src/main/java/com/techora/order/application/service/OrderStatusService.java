package com.techora.order.application.service;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderStatusService {
    private final OrderStatusUpdater orderStatusUpdater;

    @Transactional
    public Order markStockReserved(UUID orderId) {
        OrderStatusChange statusChange =
                orderStatusUpdater.update(
                        new UpdateOrderStatusCommand(
                                orderId,
                                OrderStatus.STOCK_RESERVED,
                                OrderActor.system()
                        )
                );
        return statusChange.order();
    }
}
