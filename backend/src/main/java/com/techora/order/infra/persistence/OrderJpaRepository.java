package com.techora.order.infra.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "items"})
    @Query("select orderEntity from OrderJpaEntity orderEntity where orderEntity.id = :orderId")
    Optional<OrderJpaEntity> findWithItemsById(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select orderEntity from OrderJpaEntity orderEntity
            join fetch orderEntity.user
            left join fetch orderEntity.items
            where orderEntity.id = :orderId
            """)
    Optional<OrderJpaEntity> findLockedWithItemsById(@Param("orderId") UUID orderId);

    @EntityGraph(attributePaths = {"user", "items"})
    Optional<OrderJpaEntity> findWithItemsByIdAndUserId(UUID orderId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select orderEntity from OrderJpaEntity orderEntity
            join fetch orderEntity.user
            left join fetch orderEntity.items
            where orderEntity.id = :orderId
              and orderEntity.user.id = :userId
            """)
    Optional<OrderJpaEntity> findLockedWithItemsByIdAndUserId(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId
    );

    @EntityGraph(attributePaths = {"user", "items"})
    Page<OrderJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select orderEntity from OrderJpaEntity orderEntity
            join fetch orderEntity.user
            left join fetch orderEntity.items
            where orderEntity.status = :status
              and orderEntity.paymentDeadlineAt <= :now
            order by orderEntity.paymentDeadlineAt asc
            """)
    List<OrderJpaEntity> findExpiredPaymentPendingForUpdate(
            @Param("status") com.techora.order.domain.entity.OrderStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("select orderEntity.status, count(orderEntity) from OrderJpaEntity orderEntity group by orderEntity.status")
    List<Object[]> countOrdersByStatus();
}
