package com.techora.order.infra.outbox;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.domain.event.OrderPlacedEvent;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import com.techora.order.infra.outbox.schema.OrderEventPayload;
import com.techora.order.infra.outbox.schema.OrderPlacedPayload;
import com.techora.order.infra.outbox.schema.OrderStatusChangedPayload;
import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.dto.OutboxHeaders;
import com.techora.outbox.port.OutboxEventPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderOutboxRecorder {

    private final OutboxEventPort outboxEventPort;
    private final String orderEventsTopic;

    public OrderOutboxRecorder(
            OutboxEventPort outboxEventPort,
            @Value("${app.events.order-topic:techora.order.events}") String orderEventsTopic) {
        this.outboxEventPort = outboxEventPort;
        this.orderEventsTopic = orderEventsTopic;
    }

    public void record(OrderPlacedEvent event) {
        appendOutboxRecord(
                event.orderId(),
                OutboxEventType.ORDER_PLACED,
                event.eventVersion(),
                OrderPlacedPayload.fromEvent(event)
        );
    }

    public void record(OrderStatusChangedEvent event) {
        OutboxEventType eventType = OrderStatus.isCancelled(event.newStatus())
                ? OutboxEventType.ORDER_CANCELLED
                : OutboxEventType.ORDER_STATUS_CHANGED;

        appendOutboxRecord(
                event.orderId(),
                eventType,
                event.eventVersion(),
                OrderStatusChangedPayload.from(event)
        );
    }

    private void appendOutboxRecord(UUID orderId,
                                    OutboxEventType eventType,
                                    int eventVersion,
                                    OrderEventPayload payload) {

        var record = OutboxEventRecord.builder()
                .aggregateType(OutboxAggregateType.ORDER)
                .aggregateId(orderId)
                .eventType(eventType)
                .topic(orderEventsTopic)
                .messageKey(orderId.toString())
                .eventVersion(eventVersion)
                .headers(OutboxHeaders.of(OrderOutboxHeaders.values()))
                .data(payload)
                .build();
        outboxEventPort.append(record);
    }
}
