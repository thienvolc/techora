package com.techora.inventory.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_items_product_id", columnNames = "product_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantityOnHand;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public void reduce(int quantity) {
        validatePositiveQuantity(quantity);
        if (availableQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient available quantity");
        }
        this.quantityOnHand -= quantity;
        markUpdated();
    }

    public void changeQuantityOnHand(int quantityOnHand) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }
        if (quantityOnHand < reservedQuantity) {
            throw new IllegalArgumentException("Quantity on hand cannot be less than reserved quantity");
        }
        this.quantityOnHand = quantityOnHand;
        markUpdated();
    }

    public int availableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    public void reserve(int quantity) {
        validatePositiveQuantity(quantity);
        if (availableQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient available quantity");
        }
        this.reservedQuantity += quantity;
        markUpdated();
    }

    public void confirmReserved(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.quantityOnHand -= quantity;
        markUpdated();
    }

    public void releaseReserved(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient reserved quantity");
        }
        this.reservedQuantity -= quantity;
        markUpdated();
    }

    private void markUpdated() {
        updatedAt = Instant.now();
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    @PrePersist
    @PreUpdate
    private void validateStockState() {
        if (quantityOnHand < 0 || reservedQuantity < 0 || reservedQuantity > quantityOnHand) {
            throw new IllegalStateException("Invalid inventory stock state");
        }
    }
}
