package com.techora.catalog.service;

import com.techora.catalog.application.port.inventory.CatalogInventoryPort;
import com.techora.catalog.application.port.inventory.CatalogInventoryStock;
import com.techora.catalog.application.view.ProductView;
import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.entity.CategoryEntity;
import com.techora.catalog.dto.request.CreateProductRequest;
import com.techora.catalog.dto.request.ProductRequest;
import com.techora.catalog.projection.event.ProductProjectionChangedEvent;
import com.techora.catalog.projection.event.ProductProjectionDeletedEvent;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.entity.ProductStatus;
import com.techora.catalog.mapper.ProductMapper;
import com.techora.catalog.repository.ProductRepository;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.domain.event.InternalEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final CatalogInventoryPort catalogInventoryPort;
    private final InternalEventPublisher internalEventPublisher;

    private final CategoryService categoryService;

    @Transactional
    public ProductView create(CreateProductRequest request) {
        String slug = SlugGenerator.generate(request.name());
        validateUniqueProductIdentity(request.sku(), slug);

        CategoryEntity category = categoryService.getCategoryOrThrow(request.categoryId());
        ProductStatus status = parseProductStatus(request.status());
        ProductEntity product = mapper.toEntity(request, category, slug, status);
        ProductEntity savedProduct = repository.save(product);
        CatalogInventoryStock stock = catalogInventoryPort.initializeProductStock(
                savedProduct.getId(),
                request.initialStockQuantity());
        internalEventPublisher.publish(ProductProjectionChangedEvent.of(
                mapper.toProjectionSnapshot(savedProduct, stock.availableQuantity())));
        return mapper.toView(savedProduct, stock.quantityOnHand());
    }

    @Transactional(readOnly = true)
    public ProductView get(UUID productId) {
        ProductEntity product = getProductOrThrow(productId);
        return mapper.toView(
                product,
                catalogInventoryPort.getQuantityOnHand(productId));
    }

    @Transactional
    public ProductView update(UUID productId, ProductRequest request) {
        String slug = SlugGenerator.generate(request.name());
        validateUniqueProductIdentity(
                productId,
                request.sku(),
                slug);

        ProductEntity product = getProductOrThrow(productId);
        String previousSlug = product.getSlug();
        CategoryEntity category = categoryService.getCategoryOrThrow(request.categoryId());
        ProductStatus status = parseProductStatus(request.status());
        applyProductChanges(
                product,
                request,
                category,
                slug,
                status);
        ProductEntity savedProduct = repository.save(product);
        internalEventPublisher.publish(ProductProjectionChangedEvent.of(
                mapper.toProjectionSnapshot(
                        savedProduct,
                        catalogInventoryPort.getAvailableQuantity(productId)),
                previousSlug));
        return mapper.toView(
                savedProduct,
                catalogInventoryPort.getQuantityOnHand(productId));
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
                                     String slug,
                                     ProductStatus status) {

        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(status);

        product.setCategory(category);
        product.setSlug(slug);

        product.markUpdated();
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ResponseCode.INVALID_PRODUCT_STATUS);
        }
    }
}
