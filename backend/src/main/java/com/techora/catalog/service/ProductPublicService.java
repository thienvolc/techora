package com.techora.catalog.service;

import com.techora.catalog.application.view.ProductView;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.catalog.dto.request.ProductFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPublicService {

    private final ProductReadModelService productReadModelService;

    @Transactional(readOnly = true)
    public PageResponse<ProductView> search(ProductFilter filter, Pageable pageable) {
        return productReadModelService.searchPublicProducts(filter, pageable);
    }

    @Transactional(readOnly = true)
    public ProductView getBySlug(String slug) {
        return productReadModelService.getPublicProductBySlug(slug);
    }
}
