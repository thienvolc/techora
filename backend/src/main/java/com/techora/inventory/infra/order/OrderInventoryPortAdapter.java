package com.techora.inventory.infra.order;

import com.techora.order.application.port.inventory.OrderInventoryPort;
import com.techora.order.application.port.inventory.ReserveOrderInventoryCommand;
import com.techora.order.application.port.inventory.ReserveOrderInventoryItemCommand;
import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.inventory.application.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderInventoryPortAdapter implements OrderInventoryPort {
    private final InventoryReservationService inventoryReservationService;

    @Override
    public void reserve(ReserveOrderInventoryCommand command) {
        inventoryReservationService.reserveOrder(toInventoryCommand(command));
    }

    private ReserveInventoryCommand toInventoryCommand(ReserveOrderInventoryCommand command) {
        return new ReserveInventoryCommand(
                command.orderId(),
                command.expiresAt(),
                command.items().stream()
                        .map(this::toInventoryItem)
                        .toList()
        );
    }

    private ReserveInventoryItem toInventoryItem(ReserveOrderInventoryItemCommand item) {
        return new ReserveInventoryItem(
                item.productId(),
                item.quantity()
        );
    }
}
