package com.techora.catalog.controller.request;

import com.techora.common.application.util.StringUtils;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductFilter {

    private final UUID categoryId;
    private final String keyword;

    private ProductFilter(UUID categoryId, String keyword) {
        this.categoryId = categoryId;
        this.keyword = StringUtils.trimToNull(keyword);
    }

    public static ProductFilter of(UUID categoryId, String keyword) {
        return new ProductFilter(categoryId, keyword);
    }

    public boolean hasCategory() {
        return categoryId != null;
    }

    public boolean hasKeyword() {
        return keyword != null;
    }
}