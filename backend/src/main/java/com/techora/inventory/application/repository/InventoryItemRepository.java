package com.techora.inventory.application.repository;

import com.techora.inventory.domain.entity.InventoryItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, UUID> {

    Optional<InventoryItemEntity> findByProductId(UUID productId);

    @Query("""
            select item.quantityOnHand
            from InventoryItemEntity item
            where item.productId = :productId
            """)
    Optional<Integer> findQuantityOnHandByProductId(@Param("productId") UUID productId);

    @Query("""
            select item.quantityOnHand - item.reservedQuantity
            from InventoryItemEntity item
            where item.productId = :productId
            """)
    Optional<Integer> findAvailableQuantityByProductId(@Param("productId") UUID productId);

    List<InventoryItemEntity> findByProductIdIn(Collection<UUID> productIds);

    @Query("select item.productId from InventoryItemEntity item")
    Set<UUID> findExistingProductIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItemEntity item where item.productId = :productId")
    Optional<InventoryItemEntity> findLockedByProductId(@Param("productId") UUID productId);

    Page<InventoryItemEntity> findByQuantityOnHandLessThanEqual(int threshold, Pageable pageable);
}
