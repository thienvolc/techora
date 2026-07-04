package com.techora.catalog.projection.mapper;

import com.techora.catalog.application.model.CategoryView;
import com.techora.catalog.application.model.ProductView;
import com.techora.catalog.projection.dto.CategoryProjectionSnapshot;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.projection.entity.ProductReadModelEntity;
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

    public ProductView toView(ProductReadModelEntity readModel) {
        return new ProductView(
                readModel.getProductId(),
                readModel.getName(),
                readModel.getSku(),
                readModel.getSlug(),
                readModel.getDescription(),
                readModel.getPrice(),
                readModel.getStockQuantity(),
                readModel.getStatus().name(),
                toCategoryView(readModel),
                readModel.getProductCreatedAt(),
                readModel.getProductUpdatedAt()
        );
    }

    private CategoryView toCategoryView(ProductReadModelEntity readModel) {
        return new CategoryView(
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
