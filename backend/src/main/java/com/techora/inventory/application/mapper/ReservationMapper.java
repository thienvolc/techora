package com.techora.inventory.application.mapper;

import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ReservationMapper {

    public InventoryReservationEntity toModel(ReserveInventoryItem item,
                                              ReserveInventoryCommand command,
                                              Instant now) {

        return InventoryReservationEntity.builder()
                .orderId(command.orderId())
                .productId(item.productId())
                .quantity(item.quantity())
                .status(InventoryReservationStatus.RESERVED)
                .expiresAt(command.expiresAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
