package com.techora.inventory.infra.outbox;

import com.techora.inventory.domain.event.StockReducedEvent;
import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.port.OutboxEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryOutboxEventPublisher {

    private static final String PRODUCT_ID = "productId";
    private static final String QUANTITY = "quantity";
    private static final String STOCK_QUANTITY = "stockQuantity";

    private final OutboxEventPort outboxEventPort;

    public void recordStockReduced(StockReducedEvent event) {
        outboxEventPort.append(new OutboxEventRecord(
                OutboxAggregateType.PRODUCT,
                event.productId(),
                OutboxEventType.STOCK_REDUCED,
                Map.of(
                        PRODUCT_ID, event.productId(),
                        QUANTITY, event.quantity(),
                        STOCK_QUANTITY, event.quantityOnHand()
                )
        ));
    }
}
