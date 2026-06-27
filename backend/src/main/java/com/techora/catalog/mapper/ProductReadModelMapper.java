package com.techora.catalog.mapper;

import com.techora.catalog.projection.dto.CategoryProjectionSnapshot;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.dto.response.CategoryResponse;
import com.techora.catalog.dto.response.ProductResponse;
import com.techora.catalog.entity.ProductReadModelEntity;
import org.springframework.stereotype.Service;

@Service
public class ProductReadModelMapper {

    public ProductReadModelEntity toReadModel(ProductProjectionSnapshot product) {
        CategoryProjectionSnapshot category = product.category();
        return ProductReadModelEntity.builder()
                .productId(product.id())
                .name(product.name())
                .sku(product.sku())
                .slug(product.slug())
                .description(product.description())
                .price(product.price())
                .stockQuantity(product.stockQuantity())
                .status(product.status())
                .categoryId(category.id())
                .categoryName(category.name())
                .categorySlug(category.slug())
                .categoryDescription(category.description())
                .categoryActive(category.active())
                .categoryCreatedAt(category.createdAt())
                .categoryUpdatedAt(category.updatedAt())
                .productCreatedAt(product.createdAt())
                .productUpdatedAt(product.updatedAt())
                .build();
    }

    public ProductResponse toResponse(ProductReadModelEntity readModel) {
        return new ProductResponse(
                readModel.getProductId(),
                readModel.getName(),
                readModel.getSku(),
                readModel.getSlug(),
                readModel.getDescription(),
                readModel.getPrice(),
                readModel.getStockQuantity(),
                readModel.getStatus(),
                toCategoryResponse(readModel),
                readModel.getProductCreatedAt(),
                readModel.getProductUpdatedAt()
        );
    }

    private CategoryResponse toCategoryResponse(ProductReadModelEntity readModel) {
        return new CategoryResponse(
                readModel.getCategoryId(),
                readModel.getCategoryName(),
                readModel.getCategorySlug(),
                readModel.getCategoryDescription(),
                readModel.isCategoryActive(),
                readModel.getCategoryCreatedAt(),
                readModel.getCategoryUpdatedAt()
        );
    }
}
