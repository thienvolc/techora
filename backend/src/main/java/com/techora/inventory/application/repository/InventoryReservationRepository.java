package com.techora.inventory.application.repository;

import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {
    List<InventoryReservationEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from InventoryReservationEntity reservation
            where reservation.orderId = :orderId
            order by reservation.createdAt asc
            """)
    List<InventoryReservationEntity> findLockedByOrderId(@Param("orderId") UUID orderId);

    List<InventoryReservationEntity> findByStatusAndExpiresAtBefore(
            InventoryReservationStatus status,
            Instant expiresAt
    );
}
