package com.techora.inventory.application.handler;

import com.techora.inventory.application.service.InventoryReservationService;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryOrderEventHandler {

    private final InventoryReservationService inventoryReservationService;

    @EventListener
    public void on(OrderStatusChangedEvent event) {
        if (event.newStatus() == OrderStatus.PAID) {
            inventoryReservationService.confirm(event.orderId());
            return;
        }

        if (event.newStatus() == OrderStatus.CANCELLED) {
            inventoryReservationService.release(event.orderId());
        }
    }
}
