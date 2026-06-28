package com.techora.inventory.application.handler;

import com.techora.inventory.application.service.InventoryReservationService;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryOrderEventHandler {

    private final InventoryReservationService inventoryReservationService;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(OrderStatusChangedEvent event) {
        if (event.newStatus().isPaid()) {
            inventoryReservationService.confirmOrder(event.orderId());
            return;
        }

        if (event.newStatus().isCancelled()) {
            inventoryReservationService.release(event.orderId());
        }
    }
}
