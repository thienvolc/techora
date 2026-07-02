package com.techora.catalog.mapper;

import com.techora.catalog.application.view.ProductView;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.catalog.dto.CatalogCategorySnapshot;
import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.catalog.projection.dto.CategoryProjectionSnapshot;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.entity.CategoryEntity;
import com.techora.catalog.dto.request.CreateProductRequest;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.entity.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductMapper {
    private final CategoryMapper categoryMapper;

    public ProductView toView(ProductEntity entity, int stockQuantity) {
        return new ProductView(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getPrice(),
                stockQuantity,
                entity.getStatus().name(),
                categoryMapper.toView(entity.getCategory()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PageResponse<ProductView> toPageView(Page<ProductEntity> page,
                                                Map<UUID, Integer> stockQuantities) {

        return new PageResponse<>(
                page.getContent().stream()
                        .map(product -> toView(
                                product,
                                stockQuantities.getOrDefault(product.getId(), 0)))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public CatalogProductSnapshot toCatalogSnapshot(ProductEntity entity) {
        return new CatalogProductSnapshot(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStatus().name(),
                toCategorySnapshot(entity.getCategory()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductProjectionSnapshot toProjectionSnapshot(ProductEntity entity, int stockQuantity) {
        return new ProductProjectionSnapshot(
                entity.getId(),
                entity.getName(),
                entity.getSku(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getPrice(),
                stockQuantity,
                entity.getStatus(),
                toCategoryProjectionSnapshot(entity.getCategory()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CatalogCategorySnapshot toCategorySnapshot(CategoryEntity category) {
        return new CatalogCategorySnapshot(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private CategoryProjectionSnapshot toCategoryProjectionSnapshot(CategoryEntity category) {
        return new CategoryProjectionSnapshot(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(CreateProductRequest request,
                                  CategoryEntity category,
                                  String slug,
                                  ProductStatus status) {

        Instant now = Instant.now();
        return ProductEntity.builder()
                .name(request.name())
                .sku(request.sku())
                .slug(slug)
                .description(request.description())
                .price(request.price())
                .status(status)
                .category(category)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
