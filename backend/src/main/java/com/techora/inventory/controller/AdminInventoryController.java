package com.techora.inventory.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.inventory.application.mapper.InventoryItemMapper;
import com.techora.inventory.application.service.InventoryItemService;
import com.techora.inventory.application.service.InventoryReservationReconciliationService;
import com.techora.inventory.controller.constant.InventoryPageConstant;
import com.techora.inventory.controller.request.ReduceStockRequest;
import com.techora.inventory.controller.response.InventoryReservationMismatchResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryReservationReconciliationService inventoryReservationReconciliationService;
    private final InventoryItemMapper inventoryItemMapper;
    private final ResponseFactory responseFactory;

    @GetMapping("/low-stock")
    public ResponseDto getLowStockProducts(
            @Min(0)
            @RequestParam(defaultValue = "0") int threshold,

            @Min(0)
            @RequestParam(defaultValue = InventoryPageConstant.DEFAULT_PAGE) int page,

            @Min(1)
            @Max(InventoryPageConstant.MAX_SIZE)
            @RequestParam(defaultValue = InventoryPageConstant.DEFAULT_SIZE) int size) {

        Pageable pageable = PageRequest.of(page, size)
                .withSort(InventoryPageConstant.UPDATED_AT_DESCENDING);

        return responseFactory.success(
                inventoryItemMapper.toProductResponsePage(
                        inventoryItemService.getLowStockProducts(threshold, pageable))
        );
    }

    @PutMapping("/products/{productId}/stock/decrement")
    public ResponseDto reduceStock(@PathVariable UUID productId,
                                   @Valid @RequestBody ReduceStockRequest request) {

        return responseFactory.success(
                inventoryItemMapper.toProductResponse(
                        inventoryItemService.reduceStock(productId, request.stock()))
        );
    }

    @GetMapping("/reservation-reconciliation/mismatches")
    public ResponseDto getReservationMismatches() {
        return responseFactory.success(
                inventoryReservationReconciliationService.findMismatches().stream()
                        .map(InventoryReservationMismatchResponse::from)
                        .toList()
        );
    }
}
