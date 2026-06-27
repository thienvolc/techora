package com.techora.order.infra.outbox;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.port.OutboxEventPort;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.domain.event.OrderPlacedEvent;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderOutboxEventPublisher {

    private static final String ORDER_ID = "orderId";
    private static final String USER_ID = "userId";
    private static final String OLD_STATUS = "oldStatus";
    private static final String NEW_STATUS = "newStatus";
    private static final String ORDER_STATUS = "orderStatus";
    private static final String TOTAL = "total";

    private final OutboxEventPort outboxEventPort;

    public void recordPlaced(OrderPlacedEvent event) {
        outboxEventPort.append(new OutboxEventRecord(
                OutboxAggregateType.ORDER,
                event.orderId(),
                OutboxEventType.ORDER_PLACED,
                Map.of(
                        ORDER_ID, event.orderId(),
                        USER_ID, event.userId(),
                        ORDER_STATUS, event.status().name(),
                        TOTAL, event.total()
                )
        ));
    }

    public void recordStatusChanged(OrderStatusChangedEvent event) {
        OutboxEventType eventType = OrderStatus.isCancelled(event.newStatus())
                ? OutboxEventType.ORDER_CANCELLED
                : OutboxEventType.ORDER_STATUS_CHANGED;

        outboxEventPort.append(new OutboxEventRecord(
                OutboxAggregateType.ORDER,
                event.orderId(),
                eventType,
                Map.of(
                        ORDER_ID, event.orderId(),
                        USER_ID, event.userId(),
                        OLD_STATUS, event.oldStatus().name(),
                        NEW_STATUS, event.newStatus().name()
                )
        ));
    }
}
