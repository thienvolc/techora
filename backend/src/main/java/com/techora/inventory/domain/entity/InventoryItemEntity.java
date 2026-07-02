package com.techora.inventory.domain.entity;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_items_product_id", columnNames = "product_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public int availableQuantity() {
        return quantityOnHand - reservedQuantity;
    }

    public void reduce(int quantity) {
        if (availableQuantity() < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        this.quantityOnHand -= quantity;
    }

    public void reserve(int quantity) {
        if (availableQuantity() < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        this.reservedQuantity += quantity;
    }

    public void updateQuantityOnHand(int quantityOnHand) {
        if (quantityOnHand < reservedQuantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        this.quantityOnHand = quantityOnHand;
    }

    public void confirmReserved(int quantity) {
        if (reservedQuantity < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        this.reservedQuantity -= quantity;
        this.quantityOnHand -= quantity;
    }

    public void releaseReserved(int quantity) {
        if (reservedQuantity < quantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        this.reservedQuantity -= quantity;
    }
}
