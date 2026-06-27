package com.techora.order.infra.persistence;

import com.techora.user.entity.UserEntity;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Order toDomain() {
        return Order.builder()
                .id(id)
                .userId(user.getId())
                .username(user.getUsername())
                .status(status)
                .total(total)
                .items(items.stream().map(OrderItemJpaEntity::toDomain).toList())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static OrderJpaEntity fromDomain(Order order, UserEntity user) {
        OrderJpaEntity orderEntity = OrderJpaEntity.builder()
                .id(order.getId())
                .user(user)
                .status(order.getStatus())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        order.getItems().stream()
                .map(item -> OrderItemJpaEntity.fromDomain(orderEntity, item))
                .forEach(orderEntity.getItems()::add);

        return orderEntity;
    }
}
