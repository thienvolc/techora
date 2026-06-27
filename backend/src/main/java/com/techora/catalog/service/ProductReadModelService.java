package com.techora.catalog.service;

import com.techora.catalog.dto.request.ProductFilter;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.dto.response.ProductResponse;
import com.techora.catalog.entity.ProductReadModelEntity;
import com.techora.catalog.entity.ProductStatus;
import com.techora.catalog.mapper.ProductReadModelMapper;
import com.techora.catalog.repository.ProductReadModelRepository;
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
    public PageResponse<ProductResponse> searchPublicProducts(ProductFilter filter, Pageable pageable) {
        Page<ProductReadModelEntity> products = repository.searchPublicProducts(
                ProductStatus.ACTIVE,
                filter.getCategoryId(),
                filter.getKeyword(),
                pageable);

        return new PageResponse<>(
                products.getContent().stream()
                        .map(mapper::toResponse)
                        .toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_DETAIL_BY_SLUG, key = "#slug")
    public ProductResponse getPublicProductBySlug(String slug) {
        return repository.findBySlugAndStatusAndCategoryActiveTrue(slug, ProductStatus.ACTIVE)
                .map(mapper::toResponse)
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
