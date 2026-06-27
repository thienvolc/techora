package com.techora.orderhistory.mapper;

import com.techora.orderhistory.dto.OrderHistoryRecord;
import com.techora.orderhistory.dto.response.AdminOrderHistoryResponse;
import com.techora.orderhistory.dto.response.OrderHistoryResponse;
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

    public OrderHistoryResponse toResponse(OrderHistoryEntity orderHistory) {
        return new OrderHistoryResponse(
                orderHistory.getId(),
                orderHistory.getOrderId(),
                orderHistory.getEventType(),
                orderHistory.getOldStatus(),
                orderHistory.getNewStatus(),
                orderHistory.getReason(),
                orderHistory.getCreatedAt()
        );
    }

    public AdminOrderHistoryResponse toAdminResponse(OrderHistoryEntity orderHistory) {
        return new AdminOrderHistoryResponse(
                orderHistory.getId(),
                orderHistory.getOrderId(),
                orderHistory.getEventType(),
                orderHistory.getOldStatus(),
                orderHistory.getNewStatus(),
                orderHistory.getReason(),
                orderHistory.getMetadata(),
                orderHistory.getActorType(),
                orderHistory.getActorId(),
                orderHistory.getActorName(),
                orderHistory.getCreatedAt()
        );
    }
}
