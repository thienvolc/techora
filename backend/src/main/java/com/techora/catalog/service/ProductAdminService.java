package com.techora.catalog.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.entity.CategoryEntity;
import com.techora.catalog.dto.request.CreateProductRequest;
import com.techora.catalog.dto.request.ProductRequest;
import com.techora.catalog.dto.response.ProductResponse;
import com.techora.catalog.projection.event.ProductProjectionChangedEvent;
import com.techora.catalog.projection.event.ProductProjectionDeletedEvent;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.mapper.ProductMapper;
import com.techora.catalog.repository.ProductRepository;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import com.techora.inventory.application.service.InventoryItemService;
import com.techora.inventory.application.service.InventoryStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final InventoryItemService inventoryItemService;
    private final InventoryStockQueryService inventoryStockQueryService;
    private final InternalEventPublisher internalEventPublisher;

    private final CategoryService categoryService;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String slug = SlugGenerator.generate(request.name());
        validateUniqueProductIdentity(request.sku(), slug);

        CategoryEntity category = categoryService.getCategoryOrThrow(request.categoryId());
        ProductEntity product = mapper.toEntity(request, category, slug);
        ProductEntity savedProduct = repository.save(product);
        InventoryStockSnapshot stock = inventoryItemService.initializeProductStock(
                savedProduct.getId(),
                request.initialStockQuantity());
        internalEventPublisher.publish(ProductProjectionChangedEvent.of(
                mapper.toProjectionSnapshot(savedProduct, stock.availableQuantity())));
        return mapper.toResponse(savedProduct, stock.quantityOnHand());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID productId) {
        ProductEntity product = getProductOrThrow(productId);
        return mapper.toResponse(
                product,
                inventoryStockQueryService.getQuantityOnHand(productId));
    }

    @Transactional
    public ProductResponse update(UUID productId, ProductRequest request) {
        String slug = SlugGenerator.generate(request.name());
        validateUniqueProductIdentity(
                productId,
                request.sku(),
                slug);

        ProductEntity product = getProductOrThrow(productId);
        String previousSlug = product.getSlug();
        CategoryEntity category = categoryService.getCategoryOrThrow(request.categoryId());
        applyProductChanges(
                product,
                request,
                category,
                slug);
        ProductEntity savedProduct = repository.save(product);
        internalEventPublisher.publish(ProductProjectionChangedEvent.of(
                mapper.toProjectionSnapshot(
                        savedProduct,
                        inventoryStockQueryService.getAvailableQuantity(productId)),
                previousSlug));
        return mapper.toResponse(
                savedProduct,
                inventoryStockQueryService.getQuantityOnHand(productId));
    }

    @Transactional
    public void delete(UUID productId) {
        ProductEntity product = getProductOrThrow(productId);
        String slug = product.getSlug();
        repository.delete(product);
        internalEventPublisher.publish(ProductProjectionDeletedEvent.of(productId, slug));
    }

    private ProductEntity getProductOrThrow(UUID productId) {
        return repository.findWithCategoryById(productId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    private void validateUniqueProductIdentity(String sku, String slug) {
        if (repository.existsBySkuIgnoreCaseOrSlug(sku, slug)) {
            throw new BusinessException(ResponseCode.PRODUCT_ALREADY_EXISTS);
        }
    }

    private void validateUniqueProductIdentity(UUID productId, String sku, String slug) {
        if (repository.existsBySkuIgnoreCaseOrSlugAndIdNot(sku, slug, productId)) {
            throw new BusinessException(ResponseCode.PRODUCT_ALREADY_EXISTS);
        }
    }

    private void applyProductChanges(ProductEntity product,
                                     ProductRequest request,
                                     CategoryEntity category,
                                     String slug) {

        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(request.status());

        product.setCategory(category);
        product.setSlug(slug);

        product.markUpdated();
    }
}
