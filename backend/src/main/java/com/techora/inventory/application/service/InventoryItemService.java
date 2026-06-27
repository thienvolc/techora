package com.techora.inventory.application.service;

import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.catalog.projection.event.ProductStockProjectionChangedEvent;
import com.techora.catalog.service.ProductCatalogQueryService;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.inventory.application.mapper.InventoryItemMapper;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.result.InventoryItemProductResult;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProductCatalogQueryService productCatalogQueryService;
    private final InternalEventPublisher internalEventPublisher;
    private final InventoryItemMapper inventoryItemMapper;

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemProductResult> getLowStockProducts(int threshold, Pageable pageable) {
        Page<InventoryItemEntity> items =
                inventoryItemRepository.findByQuantityOnHandLessThanEqual(threshold, pageable);
        Map<UUID, CatalogProductSnapshot> productsById = getProductsById(items);

        return new PageResponse<>(
                items.getContent().stream()
                        .map(item -> toProductResult(item, productsById))
                        .toList(),
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages()
        );
    }

    @Transactional
    public InventoryItemProductResult reduceStock(UUID productId, int quantity) {
        InventoryStockSnapshot stock = decreaseQuantityOnHand(productId, quantity);
        CatalogProductSnapshot product = productCatalogQueryService.getProduct(productId);
        return inventoryItemMapper.toProductResult(product, stock.quantityOnHand());
    }

    @Transactional
    public InventoryStockSnapshot initializeProductStock(UUID productId, int quantity) {
        return toStockSnapshot(
                changeQuantityOnHand(productId, quantity));
    }

    @Transactional
    public void reserve(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        validateAvailableQuantity(item, quantity);
        item.reserve(quantity);
        publishStockProjectionChanged(item);
    }

    @Transactional
    public InventoryStockSnapshot confirmReserved(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        try {
            item.confirmReserved(quantity);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        publishStockProjectionChanged(item);
        return toStockSnapshot(item);
    }

    @Transactional
    public void releaseReserved(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        try {
            item.releaseReserved(quantity);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        publishStockProjectionChanged(item);
    }

    private InventoryItemEntity getLockedItemOrCreate(UUID productId) {
        return inventoryItemRepository.findLockedByProductId(productId)
                .orElseGet(() -> createItemFromProduct(productId));
    }

    private InventoryItemEntity changeQuantityOnHand(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }

        InventoryItemEntity item = getLockedItemOrCreate(productId);
        try {
            item.changeQuantityOnHand(quantity);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        publishStockProjectionChanged(item);
        return item;
    }

    private InventoryStockSnapshot decreaseQuantityOnHand(UUID productId, int quantity) {
        InventoryItemEntity item = getLockedItemOrCreate(productId);
        validateAvailableQuantity(item, quantity);
        try {
            item.reduce(quantity);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
        publishStockProjectionChanged(item);
        return toStockSnapshot(item);
    }

    private InventoryItemEntity createItemFromProduct(UUID productId) {
        CatalogProductSnapshot product = productCatalogQueryService.getProduct(productId);
        Instant now = Instant.now();
        return inventoryItemRepository.save(InventoryItemEntity.builder()
                .productId(product.id())
                .quantityOnHand(0)
                .reservedQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void validateAvailableQuantity(InventoryItemEntity item, int requestedQuantity) {
        if (requestedQuantity <= 0 || item.availableQuantity() < requestedQuantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }

    private Map<UUID, CatalogProductSnapshot> getProductsById(Page<InventoryItemEntity> items) {
        return productCatalogQueryService.getProducts(items.getContent().stream()
                        .map(InventoryItemEntity::getProductId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(CatalogProductSnapshot::id, product -> product));
    }

    private InventoryItemProductResult toProductResult(InventoryItemEntity item,
                                                       Map<UUID, CatalogProductSnapshot> productsById) {

        CatalogProductSnapshot product = productsById.get(item.getProductId());
        if (product == null) {
            throw new BusinessException(ResponseCode.PRODUCT_NOT_FOUND);
        }
        return inventoryItemMapper.toProductResult(product, item.getQuantityOnHand());
    }

    private InventoryStockSnapshot toStockSnapshot(InventoryItemEntity item) {
        return new InventoryStockSnapshot(
                item.getProductId(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                item.getUpdatedAt()
        );
    }

    private void publishStockProjectionChanged(InventoryItemEntity item) {
        internalEventPublisher.publish(ProductStockProjectionChangedEvent.of(
                item.getProductId(),
                item.availableQuantity(),
                item.getUpdatedAt()));
    }
}
