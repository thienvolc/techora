package com.techora.inventory.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.inventory.application.mapper.InventoryItemMapper;
import com.techora.inventory.application.model.InventoryProductStockView;
import com.techora.inventory.application.model.InventoryStockSnapshot;
import com.techora.inventory.application.port.catalog.InventoryCatalogPort;
import com.techora.inventory.application.port.catalog.InventoryCatalogProduct;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.event.InventoryStockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryItemService {
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryCatalogPort inventoryCatalogPort;
    private final InventoryItemMapper inventoryItemMapper;
    private final InternalEventPublisher internalEventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<InventoryProductStockView> getLowStockProducts(int threshold, Pageable pageable) {
        Page<InventoryItemEntity> items = getItemsBelowThreshold(threshold, pageable);
        Map<UUID, InventoryCatalogProduct> productsById = getProductsForItems(items);
        return inventoryItemMapper.toPageView(items, productsById);
    }

    private Page<InventoryItemEntity> getItemsBelowThreshold(int threshold, Pageable pageable) {
        return inventoryItemRepository.findByQuantityOnHandLessThanEqual(threshold, pageable);
    }

    private Map<UUID, InventoryCatalogProduct> getProductsForItems(Page<InventoryItemEntity> items) {
        List<UUID> productIds = extractProductIds(items);
        Map<UUID, InventoryCatalogProduct> productsById = getProductsByIds(productIds);
        validateProductsExistForAllIds(productIds, productsById);
        return productsById;
    }

    private List<UUID> extractProductIds(Page<InventoryItemEntity> items) {
        return items.getContent().stream()
                .map(InventoryItemEntity::getProductId)
                .toList();
    }

    private Map<UUID, InventoryCatalogProduct> getProductsByIds(List<UUID> productIds) {
        return inventoryCatalogPort.getProducts(productIds).stream()
                .collect(Collectors.toMap(InventoryCatalogProduct::id, Function.identity()));
    }

    private void validateProductsExistForAllIds(List<UUID> productIds,
                                                Map<UUID, InventoryCatalogProduct> productsById) {

        if (productsById.size() != productIds.size()) {
            throw new BusinessException(ResponseCode.PRODUCT_NOT_FOUND);
        }
    }

    @Transactional
    public InventoryProductStockView reduceStock(UUID productId, int quantity) {
        InventoryCatalogProduct product = inventoryCatalogPort.getProduct(productId);

        InventoryItemEntity item = getLockedItemOrCreate(productId);
        item.reduce(quantity);

        publishInventoryStockChanged(item);
        return inventoryItemMapper.toProductStockView(product, item.getQuantityOnHand());
    }

    @Transactional
    public InventoryStockSnapshot initializeProductStock(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        item.updateQuantityOnHand(quantity);

        publishInventoryStockChanged(item);
        return inventoryItemMapper.toStockSnapshot(item);
    }


    @Transactional
    public void reserveStock(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        item.reserve(quantity);
        publishInventoryStockChanged(item);
    }

    @Transactional
    public InventoryItemEntity confirmReservedStock(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        item.confirmReserved(quantity);

        publishInventoryStockChanged(item);
        return item;
    }

    @Transactional
    public void releaseReserved(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        item.releaseReserved(quantity);
        publishInventoryStockChanged(item);
    }


    private void publishInventoryStockChanged(InventoryItemEntity item) {
        internalEventPublisher.publish(InventoryStockChangedEvent.of(
                item.getProductId(),
                item.availableQuantity(),
                item.getUpdatedAt()));
    }

    private InventoryItemEntity getLockedItemOrCreate(UUID productId) {
        return inventoryItemRepository.findLockedByProductId(productId)
                .orElseGet(() -> createItemFromProduct(productId));
    }

    // TODO: We should create inventory item when product is created
    //  not use the lazy strategy in current case it may cause race
    private InventoryItemEntity createItemFromProduct(UUID productId) {
        InventoryCatalogProduct product = inventoryCatalogPort.getProduct(productId);
        Instant now = now();
        return inventoryItemRepository.save(InventoryItemEntity.builder()
                .productId(product.id())
                .quantityOnHand(0)
                .reservedQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
