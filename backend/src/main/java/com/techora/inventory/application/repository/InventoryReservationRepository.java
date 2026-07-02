package com.techora.inventory.application.repository;

import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.application.result.InventoryReservationMismatchRow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
            with reserved as (
                select reservation.product_id, sum(reservation.quantity) as reserved_quantity
                from inventory_reservations reservation
                where reservation.status = 'RESERVED'
                group by reservation.product_id
            )
            select
                item.product_id as "productId",
                item.reserved_quantity as "stockReservedQuantity",
                coalesce(reserved.reserved_quantity, 0) as "reservationReservedQuantity",
                item.reserved_quantity - coalesce(reserved.reserved_quantity, 0) as "difference"
            from inventory_items item
            left join reserved on reserved.product_id = item.product_id
            where item.reserved_quantity <> coalesce(reserved.reserved_quantity, 0)

            union all

            select
                reserved.product_id as "productId",
                0 as "stockReservedQuantity",
                reserved.reserved_quantity as "reservationReservedQuantity",
                0 - reserved.reserved_quantity as "difference"
            from reserved
            left join inventory_items item on item.product_id = reserved.product_id
            where item.product_id is null
            """, nativeQuery = true)
    List<InventoryReservationMismatchRow> findReservationMismatches();
}
