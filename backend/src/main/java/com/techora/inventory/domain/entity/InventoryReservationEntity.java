package com.techora.inventory.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations", indexes = {
        @Index(name = "idx_inventory_reservations_order", columnList = "order_id"),
        @Index(name = "idx_inventory_reservations_product_status", columnList = "product_id,status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryReservationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isReserved() {
        return  status == InventoryReservationStatus.RESERVED;
    }

    public void markRelease() {
        this.status = InventoryReservationStatus.RELEASED;
        markUpdated();
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    public void markConfirmed() {
        this.status = InventoryReservationStatus.CONFIRMED;
        markUpdated();
    }

    public void markExpired() {
        this.status = InventoryReservationStatus.EXPIRED;
        markUpdated();
    }
}
