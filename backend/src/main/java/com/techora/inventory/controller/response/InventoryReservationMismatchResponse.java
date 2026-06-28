package com.techora.inventory.controller.response;

import com.techora.inventory.application.result.InventoryReservationMismatch;

import java.util.UUID;

public record InventoryReservationMismatchResponse(
        UUID productId,
        int stockReservedQuantity,
        int reservationReservedQuantity,
        int difference
) {
    public static InventoryReservationMismatchResponse from(InventoryReservationMismatch mismatch) {
        return new InventoryReservationMismatchResponse(
                mismatch.productId(),
                mismatch.stockReservedQuantity(),
                mismatch.reservationReservedQuantity(),
                mismatch.difference()
        );
    }
}
