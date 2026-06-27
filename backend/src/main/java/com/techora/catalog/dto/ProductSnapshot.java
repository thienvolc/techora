package com.techora.catalog.dto;

import com.techora.catalog.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshot(
        UUID id,
        String name,
        String sku,
        String slug,
        BigDecimal price,
        ProductStatus status,
        boolean categoryActive
) {

    public boolean isInactive() {
        return status == ProductStatus.INACTIVE || !categoryActive;
    }
}
