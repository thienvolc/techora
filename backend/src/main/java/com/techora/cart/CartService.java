package com.techora.cart;

import com.techora.common.application.aop.BusinessException;
import com.techora.cart.dto.checkout.CartCheckoutSnapshot;
import com.techora.cart.dto.request.AddCartItemRequest;
import com.techora.cart.dto.request.UpdateCartItemRequest;
import com.techora.cart.dto.response.CartResponse;
import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.cart.mapper.CartMapper;
import com.techora.cart.repository.CartItemRepository;
import com.techora.cart.repository.CartRepository;
import com.techora.catalog.dto.ProductSnapshot;
import com.techora.catalog.service.ProductAvailabilityService;
import com.techora.catalog.service.ProductPurchasePolicy;
import com.techora.common.application.constant.ResponseCode;
import com.techora.inventory.application.service.InventoryStockQueryService;
import com.techora.user.dto.UserSnapshot;
import com.techora.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int EMPTY_ITEM_QUANTITY = 0;

    private final CartRepository repository;
    private final CartItemRepository itemRepository;
    private final CartMapper mapper;

    private final UserService userService;
    private final ProductAvailabilityService productAvailabilityService;
    private final ProductPurchasePolicy productPurchasePolicy;
    private final InventoryStockQueryService inventoryStockQueryService;

    @Transactional
    public CartResponse get(UUID userId) {
        return mapper.toResponse(
                getOrCreateCart(userId));
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        CartEntity cart = getOrCreateCart(userId);
        ProductSnapshot product =
                productAvailabilityService
                        .getLockedAvailableSnapshotOrThrow(
                                request.productId());
        CartItemEntity item = getOrCreateItem(cart, product);

        int requestedQuantity = item.getQuantity() + request.quantity();
        applyItemQuantityChanges(
                item,
                requestedQuantity,
                product);

        cart.markUpdated();
        return mapper.toResponse(
                repository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(UUID userId,
                                   UUID itemId,
                                   UpdateCartItemRequest request) {

        CartItemEntity item = getItemOrThrow(userId, itemId);

        ProductSnapshot product = mapper.toProductSnapshot(item);
        validateProductAvailable(product);

        applyItemQuantityChanges(
                item,
                request.quantity(),
                product);

        var cart = item.getCart();
        cart.markUpdated();
        return mapper.toResponse(
                repository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        CartItemEntity item = getItemOrThrow(userId, itemId);
        CartEntity cart = item.getCart();

        cart.getItems().remove(item);

        cart.markUpdated();
        return mapper.toResponse(
                repository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);

        cart.getItems().clear();

        cart.markUpdated();
        return mapper.toResponse(
                repository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartEntity getCartOrThrow(UUID userId) {
        return getOrCreateCart(userId);
    }

    @Transactional(readOnly = true)
    public CartCheckoutSnapshot getCheckoutSnapshot(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);
        cart.getItems().forEach(this::validateCheckoutItem);
        return mapper.toCheckoutSnapshot(cart);
    }

    private CartEntity getOrCreateCart(UUID userId) {
        return repository.findWithItemsByUserId(userId)
                .orElseGet(() ->
                        createCart(userId));
    }

    private CartEntity createCart(UUID userId) {
        UserSnapshot user = userService.getSnapshotOrThrow(userId);
        return repository.save(
                mapper.toEntity(user));
    }

    private CartItemEntity getOrCreateItem(CartEntity cart, ProductSnapshot product) {
        return itemRepository
                .findByCartIdAndProductId(
                        cart.getId(), product.id())
                .orElseGet(() ->
                        createItem(cart, product));
    }

    private CartItemEntity createItem(CartEntity cart, ProductSnapshot product) {
        CartItemEntity item =
                mapper.toItemEntity(
                        cart,
                        product,
                        EMPTY_ITEM_QUANTITY);

        cart.getItems().add(item);
        return item;
    }

    private CartItemEntity getItemOrThrow(UUID userId, UUID itemId) {
        return itemRepository
                .findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CART_ITEM_NOT_FOUND));
    }

    private void applyItemQuantityChanges(CartItemEntity item,
                                          int requestedQuantity,
                                          ProductSnapshot product) {

        validateAvailableQuantity(product, requestedQuantity);

        item.setQuantity(requestedQuantity);
        item.markUpdated();
    }

    private void validateAvailableQuantity(ProductSnapshot product, int requestedQuantity) {
        inventoryStockQueryService.validateAvailableQuantity(product.id(), requestedQuantity);
    }

    private void validateProductAvailable(ProductSnapshot product) {
        productPurchasePolicy.validateAvailable(product);
    }

    private void validateCheckoutItem(CartItemEntity item) {
        ProductSnapshot product = mapper.toProductSnapshot(item);
        productPurchasePolicy.validatePurchasable(product);
        inventoryStockQueryService.validateAvailableQuantity(product.id(), item.getQuantity());
    }
}
