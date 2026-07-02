package com.techora.cart;

import com.techora.cart.dto.response.CartView;
import com.techora.common.application.aop.BusinessException;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.cart.dto.request.AddCartItemRequest;
import com.techora.cart.dto.request.UpdateCartItemRequest;
import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.cart.mapper.CartMapper;
import com.techora.cart.repository.CartItemRepository;
import com.techora.cart.repository.CartRepository;
import com.techora.catalog.dto.CatalogProductSnapshot;
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
    public CartView getCart(UUID userId) {
        return mapper.toSnapshot(
                getOrCreateCart(userId));
    }

    @Transactional
    public CartView addItem(UUID userId, AddCartItemRequest request) {
        CartEntity cart = getOrCreateCart(userId);
        CatalogProductSnapshot product =
                productAvailabilityService
                        .getLockedActiveProductOrThrow(
                                request.productId());
        CartItemEntity item = getOrCreateItem(cart, product);

        int requestedQuantity = item.getQuantity() + request.quantity();
        applyItemQuantityChanges(
                item,
                requestedQuantity,
                product);

        cart.markUpdated();
        return mapper.toSnapshot(
                repository.save(cart));
    }

    @Transactional
    public CartView updateItem(UUID userId,
                               UUID itemId,
                               UpdateCartItemRequest request) {

        CartItemEntity item = getItemOrThrow(userId, itemId);

        CatalogProductSnapshot product = mapper.toCatalogProductSnapshot(item);
        validateProductAvailable(product);

        applyItemQuantityChanges(
                item,
                request.quantity(),
                product);

        var cart = item.getCart();
        cart.markUpdated();
        return mapper.toSnapshot(
                repository.save(cart));
    }

    @Transactional
    public CartView removeItem(UUID userId, UUID itemId) {
        CartItemEntity item = getItemOrThrow(userId, itemId);
        CartEntity cart = item.getCart();

        cart.getItems().remove(item);

        cart.markUpdated();
        return mapper.toSnapshot(
                repository.save(cart));
    }

    @Transactional
    public CartView clearCart(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);

        cart.getItems().clear();

        cart.markUpdated();
        return mapper.toSnapshot(
                repository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartSnapshot getPlaceOrderCartSnapshot(UUID userId) {
        CartEntity cart = getOrCreateCart(userId);
        cart.getItems().forEach(this::validatePlaceOrderItem);
        return mapper.toCartSnapshot(cart);
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

    private CartItemEntity getOrCreateItem(CartEntity cart, CatalogProductSnapshot product) {
        return itemRepository
                .findByCartIdAndProductId(
                        cart.getId(), product.id())
                .orElseGet(() ->
                        createItem(cart, product));
    }

    private CartItemEntity createItem(CartEntity cart, CatalogProductSnapshot product) {
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
                                          CatalogProductSnapshot product) {

        validateAvailableQuantity(product, requestedQuantity);

        item.setQuantity(requestedQuantity);
        item.markUpdated();
    }

    private void validateAvailableQuantity(CatalogProductSnapshot product, int requestedQuantity) {
        inventoryStockQueryService.validateAvailableQuantity(product.id(), requestedQuantity);
    }

    private void validateProductAvailable(CatalogProductSnapshot product) {
        productPurchasePolicy.validateAvailable(product);
    }

    private void validatePlaceOrderItem(CartItemEntity item) {
        CatalogProductSnapshot product = mapper.toCatalogProductSnapshot(item);
        productPurchasePolicy.validatePurchasable(product);
        inventoryStockQueryService.validateAvailableQuantity(product.id(), item.getQuantity());
    }
}
