package com.techora.order.application.port.inventory;

public interface OrderInventoryPort {
    void reserve(ReserveOrderInventoryCommand command);
}
