package com.techora.catalog.application.service;

import com.techora.catalog.application.model.CatalogProductSnapshot;
import com.techora.catalog.persistence.mapper.ProductMapper;
import com.techora.catalog.persistence.repository.ProductRepository;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductCatalogQueryService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public CatalogProductSnapshot getProduct(UUID productId) {
        return repository.findWithCategoryById(productId)
                .map(mapper::toCatalogSnapshot)
                .orElseThrow(() -> new BusinessException(ResponseCode.PRODUCT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<CatalogProductSnapshot> getProducts(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        return repository.findWithCategoryByIdIn(productIds).stream()
                .map(mapper::toCatalogSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogProductSnapshot> getAllProducts() {
        return repository.findAllBy().stream()
                .map(mapper::toCatalogSnapshot)
                .toList();
    }
}
