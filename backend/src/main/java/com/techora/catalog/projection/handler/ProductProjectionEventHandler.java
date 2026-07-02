package com.techora.catalog.projection.handler;

import com.techora.catalog.application.port.inventory.CatalogInventoryPort;
import com.techora.catalog.projection.event.CategoryProjectionChangedEvent;
import com.techora.catalog.projection.event.ProductProjectionChangedEvent;
import com.techora.catalog.projection.event.ProductProjectionDeletedEvent;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.mapper.ProductMapper;
import com.techora.catalog.repository.ProductRepository;
import com.techora.catalog.service.ProductReadModelService;
import com.techora.common.infra.cache.CacheEvictionService;
import com.techora.common.infra.cache.CacheNames;
import com.techora.inventory.domain.event.InventoryStockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductProjectionEventHandler {
    private final ProductReadModelService productReadModelService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CatalogInventoryPort catalogInventoryPort;
    private final CacheEvictionService cacheEvictionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductProjectionChangedEvent event) {
        productReadModelService.upsert(event.product());
        evictProductDetail(event.product().slug());
        evictProductDetail(event.previousSlug());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ProductProjectionDeletedEvent event) {
        productReadModelService.delete(event.productId());
        evictProductDetail(event.slug());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(InventoryStockChangedEvent event) {
        productReadModelService.updateStockQuantity(
                event.productId(),
                event.availableQuantity())
                .ifPresent(this::evictProductDetail);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CategoryProjectionChangedEvent event) {
        var products = productRepository.findWithCategoryByCategoryId(event.categoryId());
        Map<UUID, Integer> stockByProductId = catalogInventoryPort.getAvailableQuantities(
                products.stream()
                        .map(ProductEntity::getId)
                        .toList());

        productReadModelService.upsertAll(products.stream()
                .map(product -> productMapper.toProjectionSnapshot(
                        product,
                        stockByProductId.getOrDefault(product.getId(), 0)))
                .toList());
        products.forEach(product -> evictProductDetail(product.getSlug()));
        evictActiveCategories();
    }

    private void evictProductDetail(String slug) {
        cacheEvictionService.evict(CacheNames.PRODUCT_DETAIL_BY_SLUG, slug);
    }

    private void evictActiveCategories() {
        cacheEvictionService.clear(CacheNames.ACTIVE_CATEGORIES);
    }
}
