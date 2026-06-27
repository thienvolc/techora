package com.techora.order.infra.persistence;

import com.techora.order.domain.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 160)
    private String productName;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    public static OrderItemJpaEntity fromDomain(OrderJpaEntity order, OrderItem item) {
        return OrderItemJpaEntity.builder()
                .id(item.getId())
                .order(order)
                .productId(item.getProductId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    public OrderItem toDomain() {
        return OrderItem.builder()
                .id(id)
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(subtotal)
                .build();
    }
}
