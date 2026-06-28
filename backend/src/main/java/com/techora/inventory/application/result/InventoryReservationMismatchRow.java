package com.techora.inventory.application.result;

import java.util.UUID;

public interface InventoryReservationMismatchRow {
    UUID getProductId();

    Number getStockReservedQuantity();

    Number getReservationReservedQuantity();

    Number getDifference();
}
