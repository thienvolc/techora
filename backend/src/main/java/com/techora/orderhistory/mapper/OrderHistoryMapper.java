package com.techora.orderhistory.mapper;

import com.techora.orderhistory.application.view.AdminOrderHistoryView;
import com.techora.orderhistory.application.view.OrderHistoryView;
import com.techora.orderhistory.dto.OrderHistoryRecord;
import com.techora.orderhistory.entity.OrderHistoryEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderHistoryMapper {

    public OrderHistoryEntity toEntity(OrderHistoryRecord dto) {
        return OrderHistoryEntity.builder()
                .orderId(dto.orderId())
                .ownerUserId(dto.ownerUserId())
                .eventType(dto.eventType())
                .oldStatus(dto.oldStatus())
                .newStatus(dto.newStatus())
                .reason(dto.reason().getValue())
                .metadata(dto.metadata())
                .actorType(dto.actorType())
                .actorId(dto.actorId())
                .actorName(dto.actorName())
                .createdAt(Instant.now())
                .build();
    }

    public OrderHistoryView toView(OrderHistoryEntity orderHistory) {
        return new OrderHistoryView(
                orderHistory.getId(),
                orderHistory.getOrderId(),
                enumName(orderHistory.getEventType()),
                enumName(orderHistory.getOldStatus()),
                enumName(orderHistory.getNewStatus()),
                orderHistory.getReason(),
                orderHistory.getCreatedAt()
        );
    }

    public AdminOrderHistoryView toAdminView(OrderHistoryEntity orderHistory) {
        return new AdminOrderHistoryView(
                orderHistory.getId(),
                orderHistory.getOrderId(),
                enumName(orderHistory.getEventType()),
                enumName(orderHistory.getOldStatus()),
                enumName(orderHistory.getNewStatus()),
                orderHistory.getReason(),
                orderHistory.getMetadata(),
                enumName(orderHistory.getActorType()),
                orderHistory.getActorId(),
                orderHistory.getActorName(),
                orderHistory.getCreatedAt()
        );
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
