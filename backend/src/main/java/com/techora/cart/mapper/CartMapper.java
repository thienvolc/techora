package com.techora.cart.mapper;

import com.techora.cart.dto.response.CartItemResponse;
import com.techora.cart.dto.response.CartResponse;
import com.techora.cart.dto.checkout.CartCheckoutItem;
import com.techora.cart.dto.checkout.CartCheckoutSnapshot;
import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.catalog.dto.ProductSnapshot;
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

    public CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items =
                cart.getItems().stream()
                        .map(this::toItemResponse)
                        .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .total(total)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartCheckoutSnapshot toCheckoutSnapshot(CartEntity cart) {
        return new CartCheckoutSnapshot(
                cart.getUser().getId(),
                cart.getUser().getUsername(),
                cart.getItems().stream()
                        .map(this::toCheckoutItem)
                        .toList()
        );
    }

    private CartItemResponse toItemResponse(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .slug(product.getSlug())
                .unitPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.subtotal())
                .build();
    }

    private CartCheckoutItem toCheckoutItem(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return new CartCheckoutItem(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                item.getQuantity()
        );
    }

    public ProductSnapshot toProductSnapshot(CartItemEntity item) {
        ProductEntity product = item.getProduct();

        return new ProductSnapshot(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getSlug(),
                product.getPrice(),
                product.getStatus(),
                product.getCategory().isActive()
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
                                       ProductSnapshot product,
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
