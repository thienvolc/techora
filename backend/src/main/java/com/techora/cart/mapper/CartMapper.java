package com.techora.cart.mapper;

import com.techora.cart.dto.response.CartItemView;
import com.techora.cart.dto.response.CartView;
import com.techora.cart.dto.order.CartItemSnapshot;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.catalog.dto.CatalogCategorySnapshot;
import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.catalog.entity.ProductEntity;
import com.techora.user.dto.UserSnapshot;
import com.techora.user.entity.UserEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartMapper {

    private final EntityManager entityManager;

    public CartView toSnapshot(CartEntity cart) {
        List<CartItemView> items =
                cart.getItems().stream()
                        .map(this::toItemSnapshot)
                        .toList();

        BigDecimal total = items.stream()
                .map(CartItemView::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartView(
                cart.getId(),
                cart.getUser().getId(),
                items,
                total,
                cart.getUpdatedAt());
    }

    public CartSnapshot toCartSnapshot(CartEntity cart) {
        return new CartSnapshot(
                cart.getUser().getId(),
                cart.getUser().getUsername(),
                cart.getItems().stream()
                        .map(this::toCartItem)
                        .toList()
        );
    }

    private CartItemView toItemSnapshot(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return new CartItemView(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getSlug(),
                product.getPrice(),
                item.getQuantity(),
                item.subtotal());
    }

    private CartItemSnapshot toCartItem(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return new CartItemSnapshot(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                item.getQuantity()
        );
    }

    public CatalogProductSnapshot toCatalogProductSnapshot(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return new CatalogProductSnapshot(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus().name(),
                new CatalogCategorySnapshot(
                        product.getCategory().getId(),
                        product.getCategory().getName(),
                        product.getCategory().getSlug(),
                        product.getCategory().getDescription(),
                        product.getCategory().isActive(),
                        product.getCategory().getCreatedAt(),
                        product.getCategory().getUpdatedAt()
                ),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public CartEntity toEntity(UserSnapshot user) {
        Instant now = Instant.now();
        return CartEntity.builder()
                .user(entityManager.getReference(UserEntity.class, user.id()))
                .updatedAt(now)
                .createdAt(now)
                .build();
    }

    public CartItemEntity toItemEntity(CartEntity cart,
                                       CatalogProductSnapshot product,
                                       int quantity) {

        Instant now = Instant.now();
        return CartItemEntity.builder()
                .cart(cart)
                .product(entityManager.getReference(ProductEntity.class, product.id()))
                .quantity(quantity)
                .updatedAt(now)
                .createdAt(now)
                .build();
    }
}
