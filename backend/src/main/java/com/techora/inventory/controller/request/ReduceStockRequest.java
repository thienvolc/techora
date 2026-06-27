package com.techora.inventory.controller.request;

import jakarta.validation.constraints.Min;

public record ReduceStockRequest(
        @Min(1) int stock
) {
}
