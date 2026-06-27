package com.techora.cart.repository;

import com.techora.cart.entity.CartItemEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    @EntityGraph(attributePaths = {"cart"})
    Optional<CartItemEntity> findByCartIdAndProductId(UUID cartId, UUID productId);

    @EntityGraph(attributePaths = {"product", "product.category", "cart"})
    Optional<CartItemEntity> findByIdAndCartUserId(UUID id, UUID userId);
}
