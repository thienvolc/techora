package com.techora.orderhistory.handler;

import com.techora.order.domain.event.OrderPlacedEvent;
import com.techora.order.domain.event.OrderStatusChangedEvent;
import com.techora.orderhistory.dto.OrderHistoryRecord;
import com.techora.orderhistory.OrderHistoryService;
import com.techora.orderhistory.entity.OrderHistoryActorType;
import com.techora.orderhistory.entity.OrderHistoryEventType;
import com.techora.orderhistory.entity.OrderReasons;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderHistoryEventHandler {

    private static final String EMPTY_METADATA = "{}";

    private final OrderHistoryService orderHistoryService;

    @EventListener
    public void on(OrderPlacedEvent event) {
        var dto = OrderHistoryRecord.builder()
                .orderId(event.orderId())
                .ownerUserId(event.userId())
                .eventType(OrderHistoryEventType.ORDER_PLACED)
                .newStatus(event.status())
                .reason(OrderReasons.PLACE_ORDER_COMPLETED)
                .metadata(EMPTY_METADATA)
                .actorType(OrderHistoryActorType.USER)
                .actorId(event.userId())
                .actorName(event.username())
                .build();

        orderHistoryService.record(dto);
    }

    @EventListener
    public void on(OrderStatusChangedEvent event) {
        var dto = OrderHistoryRecord.builder()
                .orderId(event.orderId())
                .ownerUserId(event.userId())
                .eventType(OrderHistoryEventType.of(event.newStatus()))
                .newStatus(event.newStatus())
                .oldStatus(event.oldStatus())
                .reason(reasonOf(event))
                .metadata(EMPTY_METADATA)
                .actorType(OrderHistoryActorType.of(event.actorType()))
                .actorId(event.actorId())
                .actorName(event.actorName())
                .build();

        orderHistoryService.record(dto);
    }

    private OrderReasons reasonOf(OrderStatusChangedEvent event) {
        return switch (event.actorType()) {
            case USER -> OrderReasons.USER_PAYMENT_UPDATE;
            case ADMIN -> OrderReasons.ADMIN_STATUS_UPDATE;
            case SYSTEM -> OrderReasons.SYSTEM_STATUS_UPDATE;
        };
    }
}
