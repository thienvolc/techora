package com.techora.inventory.infra.outbox;

import com.techora.inventory.domain.event.StockReducedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryOutboxListener {

    private final InventoryOutboxRecorder outboxRecorder;

    @EventListener
    public void on(StockReducedEvent event) {
        outboxRecorder.record(event);
    }
}
