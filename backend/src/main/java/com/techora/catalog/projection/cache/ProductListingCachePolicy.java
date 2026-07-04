package com.techora.catalog.projection.cache;

import com.techora.catalog.controller.constant.ProductPageConstant;
import com.techora.catalog.controller.request.ProductFilter;
import org.springframework.data.domain.Pageable;

public final class ProductListingCachePolicy {

    private static final int DEFAULT_SIZE = Integer.parseInt(ProductPageConstant.DEFAULT_SIZE);

    private ProductListingCachePolicy() {
    }

    public static boolean isCacheable(ProductFilter filter, Pageable pageable) {
        return !filter.hasKeyword()
                && pageable.getPageNumber() == 0
                && pageable.getPageSize() == DEFAULT_SIZE
                && pageable.getSort().equals(ProductPageConstant.CREATED_AT_DESCENDING);
    }
}
