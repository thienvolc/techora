package com.techora.catalog.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.catalog.service.CategoryService;
import com.techora.catalog.dto.request.CategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final ResponseFactory responseFactory;

    @PostMapping
    public ResponseDto create(@Valid @RequestBody CategoryRequest request) {
        return responseFactory.success(categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    public ResponseDto update(@PathVariable UUID categoryId,
                              @Valid @RequestBody CategoryRequest request) {

        return responseFactory.success(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseDto delete(@PathVariable UUID categoryId) {
        categoryService.delete(categoryId);
        return responseFactory.success();
    }

    @GetMapping
    public ResponseDto getAll() {
        return responseFactory.success(categoryService.getAll());
    }
}
