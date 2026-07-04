package com.techora.catalog.application.service;


import com.techora.catalog.application.model.CatalogProductSnapshot;
import com.techora.catalog.domain.entity.ProductEntity;
import com.techora.catalog.persistence.mapper.ProductMapper;
import com.techora.catalog.persistence.repository.ProductRepository;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductAvailabilityService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public CatalogProductSnapshot getLockedActiveProductOrThrow(UUID productId) {
        ProductEntity product = getLockedProductOrThrow(productId);
        validateActive(product);
        return productMapper.toCatalogSnapshot(product);
    }

    private void validateActive(ProductEntity product) {
        if (product.isInactive()) {
            throw new BusinessException(ResponseCode.PRODUCT_UNAVAILABLE);
        }
    }

    private ProductEntity getLockedProductOrThrow(UUID productId) {
        return productRepository.findLockedWithCategoryById(productId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }
}
