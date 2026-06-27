package com.techora.orderhistory.repository;

import com.techora.orderhistory.entity.OrderHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntity, UUID> {
    List<OrderHistoryEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    List<OrderHistoryEntity> findByOrderIdAndOwnerUserIdOrderByCreatedAtAsc(UUID orderId, UUID ownerUserId);
}
