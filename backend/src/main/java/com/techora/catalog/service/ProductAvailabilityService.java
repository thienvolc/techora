package com.techora.catalog.service;


import com.techora.catalog.dto.ProductSnapshot;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.mapper.ProductMapper;
import com.techora.catalog.repository.ProductRepository;
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
    public ProductSnapshot getLockedActiveProductOrThrow(UUID productId) {
        ProductEntity product = getLockedProductOrThrow(productId);
        validateActive(product);
        return productMapper.toSnapshot(product);
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
