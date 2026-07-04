package com.techora.inventory.infra.outbox;

import com.techora.inventory.domain.event.StockReducedEvent;
import com.techora.inventory.infra.outbox.schema.InventoryEventPayload;
import com.techora.inventory.infra.outbox.schema.StockReducedPayload;
import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.dto.OutboxHeaders;
import com.techora.outbox.port.OutboxEventPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryOutboxRecorder {

    private final String orderEventsTopic;
    private final OutboxEventPort outboxEventPort;

    public InventoryOutboxRecorder(
            OutboxEventPort outboxEventPort,
            @Value("${app.events.inventory-topic:techora.inventory.events}")
            String orderEventsTopic) {

        this.outboxEventPort = outboxEventPort;
        this.orderEventsTopic = orderEventsTopic;
    }

    public void record(StockReducedEvent event) {
        appendOutboxRecord(
                event.productId(),
                OutboxEventType.STOCK_REDUCED,
                event.eventVersion(),
                StockReducedPayload.from(event)
        );
    }

    private void appendOutboxRecord(UUID productId,
                                    OutboxEventType eventType,
                                    int eventVersion,
                                    InventoryEventPayload payload) {

        var record = OutboxEventRecord.builder()
                .aggregateType(OutboxAggregateType.PRODUCT)
                .aggregateId(productId)
                .eventType(eventType)
                .topic(orderEventsTopic)
                .messageKey(productId.toString())
                .eventVersion(eventVersion)
                .headers(OutboxHeaders.of(InventoryOutboxHeaders.values()))
                .data(payload)
                .build();
        outboxEventPort.append(record);
    }
}
