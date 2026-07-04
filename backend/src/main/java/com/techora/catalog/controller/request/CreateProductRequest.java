package com.techora.catalog.controller.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int initialStockQuantity,
        @NotNull UUID categoryId,
        String status
) {
    public String status() {
        return status == null ? "ACTIVE" : status;
    }
}
