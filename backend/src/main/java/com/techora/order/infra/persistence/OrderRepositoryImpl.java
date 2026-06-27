package com.techora.order.infra.persistence;

import com.techora.user.entity.UserEntity;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public Order save(Order order) {
        UserEntity user = entityManager.getReference(UserEntity.class, order.getUserId());
        return jpaRepository.save(OrderJpaEntity.fromDomain(order, user)).toDomain();
    }

    @Override
    public Optional<Order> findWithItemsById(UUID orderId) {
        return jpaRepository.findWithItemsById(orderId).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Optional<Order> findLockedWithItemsById(UUID orderId) {
        return jpaRepository.findLockedWithItemsById(orderId).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Optional<Order> findWithItemsByIdAndUserId(UUID orderId, UUID userId) {
        return jpaRepository.findWithItemsByIdAndUserId(orderId, userId).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Optional<Order> findLockedWithItemsByIdAndUserId(UUID orderId, UUID userId) {
        return jpaRepository.findLockedWithItemsByIdAndUserId(orderId, userId).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Page<Order> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Map<OrderStatus, Long> countOrdersByStatus() {
        return jpaRepository.countOrdersByStatus().stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> (Long) row[1]
                ));
    }
}
