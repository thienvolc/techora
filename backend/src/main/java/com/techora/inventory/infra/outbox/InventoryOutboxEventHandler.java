package com.techora.inventory.infra.outbox;

import com.techora.inventory.domain.event.StockReducedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryOutboxEventHandler {

    private final InventoryOutboxEventPublisher inventoryOutboxEventPublisher;

    @EventListener
    public void on(StockReducedEvent event) {
        inventoryOutboxEventPublisher.recordStockReduced(event);
    }
}
