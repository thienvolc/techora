package com.techora.catalog.dto.request;

import com.techora.catalog.entity.ProductStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotNull UUID categoryId,
        ProductStatus status
) {
    public ProductStatus status() {
        return status == null ? ProductStatus.ACTIVE : status;
    }
}
