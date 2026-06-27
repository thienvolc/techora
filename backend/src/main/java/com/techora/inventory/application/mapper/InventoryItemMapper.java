package com.techora.inventory.application.mapper;

import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.catalog.dto.CatalogCategorySnapshot;
import com.techora.catalog.dto.response.CategoryResponse;
import com.techora.catalog.dto.response.ProductResponse;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.inventory.application.result.InventoryItemProductResult;
import org.springframework.stereotype.Service;

@Service
public class InventoryItemMapper {

    public InventoryItemProductResult toProductResult(CatalogProductSnapshot product, int quantityOnHand) {
        return new InventoryItemProductResult(
                product.id(),
                product.name(),
                product.sku(),
                product.slug(),
                product.description(),
                product.price(),
                quantityOnHand,
                product.status(),
                product.category(),
                product.createdAt(),
                product.updatedAt()
        );
    }

    public ProductResponse toProductResponse(InventoryItemProductResult result) {
        return new ProductResponse(
                result.productId(),
                result.name(),
                result.sku(),
                result.slug(),
                result.description(),
                result.price(),
                result.quantityOnHand(),
                result.status(),
                toCategoryResponse(result.category()),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private CategoryResponse toCategoryResponse(CatalogCategorySnapshot category) {
        return new CategoryResponse(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.active(),
                category.createdAt(),
                category.updatedAt()
        );
    }

    public PageResponse<ProductResponse> toProductResponsePage(PageResponse<InventoryItemProductResult> page) {
        return new PageResponse<>(
                page.items().stream()
                        .map(this::toProductResponse)
                        .toList(),
                page.page(),
                page.size(),
                page.totalItems(),
                page.totalPages()
        );
    }
}
