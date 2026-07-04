package com.techora.inventory.application.model;

import java.util.UUID;

public record InventoryReservationMismatch(
        UUID productId,
        int stockReservedQuantity,
        int reservationReservedQuantity,
        int difference
) {
    public static InventoryReservationMismatch of(UUID productId,
                                                  int stockReservedQuantity,
                                                  int reservationReservedQuantity) {

        return new InventoryReservationMismatch(
                productId,
                stockReservedQuantity,
                reservationReservedQuantity,
                stockReservedQuantity - reservationReservedQuantity
        );
    }

    public static InventoryReservationMismatch from(InventoryReservationMismatchRow row) {
        return new InventoryReservationMismatch(
                row.getProductId(),
                row.getStockReservedQuantity().intValue(),
                row.getReservationReservedQuantity().intValue(),
                row.getDifference().intValue()
        );
    }
}
