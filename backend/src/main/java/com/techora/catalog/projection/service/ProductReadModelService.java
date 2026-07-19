package com.techora.catalog.projection.service;

import com.techora.catalog.application.model.ProductView;
import com.techora.catalog.controller.request.ProductFilter;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.projection.entity.ProductReadModelEntity;
import com.techora.catalog.domain.valueobject.ProductStatus;
import com.techora.catalog.projection.mapper.ProductReadModelMapper;
import com.techora.catalog.projection.repository.ProductReadModelRepository;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.infra.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReadModelService {

    private final ProductReadModelRepository repository;
    private final ProductReadModelMapper mapper;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.PRODUCT_LISTING,
            key = "T(com.techora.catalog.projection.cache.ProductListingCacheKey).of(#filter, #pageable)",
            condition = "T(com.techora.catalog.projection.cache.ProductListingCachePolicy).isCacheable(#filter, #pageable)"
    )
    public PageResponse<ProductView> searchPublicProducts(ProductFilter filter, Pageable pageable) {
        Page<ProductReadModelEntity> products = findPublicProducts(filter, pageable);

        return new PageResponse<>(
                products.getContent().stream()
                        .map(mapper::toView)
                        .toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
    }

    private Page<ProductReadModelEntity> findPublicProducts(ProductFilter filter, Pageable pageable) {
        if (filter.hasKeyword()) {
            return repository.searchPublicProductsByKeyword(
                    ProductStatus.ACTIVE,
                    filter.getCategoryId(),
                    filter.getKeyword(),
                    pageable);
        }

        return repository.searchPublicProducts(
                ProductStatus.ACTIVE,
                filter.getCategoryId(),
                pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_DETAIL_BY_SLUG, key = "#slug", sync = true)
    public ProductView getPublicProductBySlug(String slug) {
        return repository.findBySlugAndStatusAndCategoryActiveTrue(slug, ProductStatus.ACTIVE)
                .map(mapper::toView)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public void upsert(ProductProjectionSnapshot product) {
        repository.save(mapper.toReadModel(product));
    }

    @Transactional
    public void upsertAll(Collection<ProductProjectionSnapshot> products) {
        repository.saveAll(products.stream()
                .map(mapper::toReadModel)
                .toList());
    }

    @Transactional
    public Optional<String> updateStockQuantity(UUID productId, int stockQuantity) {
        return repository.findById(productId)
                .map(readModel -> {
                    readModel.updateStockQuantity(stockQuantity);
                    return readModel.getSlug();
                });
    }

    @Transactional
    public void delete(UUID productId) {
        repository.deleteById(productId);
    }
}
