package com.techora.order.application.port.persistence;

import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findWithItemsById(UUID orderId);

    Optional<Order> findLockedWithItemsById(UUID orderId);

    Optional<Order> findWithItemsByIdAndUserId(UUID orderId, UUID userId);

    Optional<Order> findLockedWithItemsByIdAndUserId(UUID orderId, UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    List<Order> findExpiredPaymentPendingForUpdate(Instant now, int limit);

    Map<OrderStatus, Long> countOrdersByStatus();
}
