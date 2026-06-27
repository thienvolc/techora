package com.techora.catalog.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.catalog.service.ProductAdminService;
import com.techora.catalog.dto.request.CreateProductRequest;
import com.techora.catalog.dto.request.ProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
    private final ProductAdminService productAdminService;
    private final ResponseFactory responseFactory;

    @PostMapping
    public ResponseDto create(@Valid @RequestBody CreateProductRequest request) {
        return responseFactory.success(productAdminService.create(request));
    }

    @GetMapping("/{productId}")
    public ResponseDto getById(@PathVariable UUID productId) {
        return responseFactory.success(productAdminService.get(productId));
    }

    @PutMapping("/{productId}")
    public ResponseDto update(@PathVariable UUID productId, @Valid @RequestBody ProductRequest request) {
        return responseFactory.success(productAdminService.update(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseDto delete(@PathVariable UUID productId) {
        productAdminService.delete(productId);
        return responseFactory.success();
    }
}
