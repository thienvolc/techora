package com.techora.catalog.controller;

import com.techora.common.application.dto.response.ResponseDto;
import com.techora.common.application.service.ResponseFactory;
import com.techora.catalog.dto.constant.ProductPageConstant;
import com.techora.catalog.service.ProductPublicService;
import com.techora.catalog.dto.request.ProductFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductPublicService productService;
    private final ResponseFactory responseFactory;

    @GetMapping
    public ResponseDto getPublicProducts(
            @RequestParam(required = false) UUID categoryId,

            @RequestParam(required = false) String keyword,

            @Min(0)
            @RequestParam(defaultValue = ProductPageConstant.DEFAULT_PAGE) int page,

            @Min(1)
            @Max(ProductPageConstant.MAX_SIZE)
            @RequestParam(defaultValue = ProductPageConstant.DEFAULT_SIZE) int size) {

        ProductFilter filter = ProductFilter.of(categoryId, keyword);
        Pageable pageable = PageRequest.of(page, size)
                .withSort(ProductPageConstant.CREATED_AT_DESCENDING);

        return responseFactory.success(
                productService.search(filter, pageable)
        );
    }

    @GetMapping("/{slug}")
    public ResponseDto getPublicProduct(@PathVariable String slug) {
        return responseFactory.success(
                productService.getBySlug(slug)
        );
    }
}
