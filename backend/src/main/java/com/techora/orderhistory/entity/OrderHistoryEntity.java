package com.techora.orderhistory.entity;

import com.techora.order.domain.entity.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_events", indexes = {
        @Index(name = "idx_order_events_order_created_at", columnList = "order_id, created_at"),
        @Index(name = "idx_order_events_owner_order_created_at", columnList = "owner_user_id, order_id, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private OrderHistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private OrderStatus newStatus;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private OrderHistoryActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name", length = 80)
    private String actorName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
