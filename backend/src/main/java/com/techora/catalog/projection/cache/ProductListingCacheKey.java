package com.techora.catalog.projection.cache;

import com.techora.catalog.controller.request.ProductFilter;
import org.springframework.data.domain.Pageable;

import java.util.Locale;

public final class ProductListingCacheKey {

    private ProductListingCacheKey() {
    }

    public static String of(ProductFilter filter, Pageable pageable) {
        return "category=%s|keyword=%s|page=%d|size=%d|sort=%s".formatted(
                filter.getCategoryId() == null ? "all" : filter.getCategoryId(),
                filter.getKeyword() == null ? "" : filter.getKeyword().toLowerCase(Locale.ROOT),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString()
        );
    }
}
