package com.techora.cart.entity;

import com.techora.catalog.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    public BigDecimal subtotal() {
        return this.product.getPrice()
                .multiply(BigDecimal.valueOf(this.quantity));
    }
}
