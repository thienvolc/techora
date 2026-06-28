package com.techora.common.infra.cache;

import java.util.Set;

public final class CacheNames {

    public static final String PRODUCT_DETAIL_BY_SLUG = "product-detail-by-slug";
    public static final String PRODUCT_LISTING = "product-listing";
    public static final String ACTIVE_CATEGORIES = "active-categories";

    public static Set<String> all() {
        return Set.of(
                PRODUCT_DETAIL_BY_SLUG,
                PRODUCT_LISTING,
                ACTIVE_CATEGORIES
        );
    }

    private CacheNames() {
    }
}
