package com.techora.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_read_models", indexes = {
        @Index(name = "idx_product_read_models_slug", columnList = "slug"),
        @Index(name = "idx_product_read_models_category", columnList = "category_id"),
        @Index(name = "idx_product_read_models_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReadModelEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 120)
    private String categoryName;

    @Column(nullable = false, length = 140)
    private String categorySlug;

    @Column(length = 500)
    private String categoryDescription;

    @Column(nullable = false)
    private boolean categoryActive;

    @Column(nullable = false)
    private Instant categoryCreatedAt;

    @Column(nullable = false)
    private Instant categoryUpdatedAt;

    @Column(nullable = false)
    private Instant productCreatedAt;

    @Column(nullable = false)
    private Instant productUpdatedAt;

    public void updateStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.stockQuantity = stockQuantity;
    }
}
