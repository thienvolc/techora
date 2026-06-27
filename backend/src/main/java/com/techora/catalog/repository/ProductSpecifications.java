package com.techora.catalog.repository;

import com.techora.catalog.dto.request.ProductFilter;
import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.entity.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    public static Specification<ProductEntity> publicProducts(ProductFilter filter) {
        return Specification
                .where(activeProduct())
                .and(activeCategory())
                .and(inCategory(filter))
                .and(nameContains(filter));
    }

    private static Specification<ProductEntity> activeProduct() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    private static Specification<ProductEntity> activeCategory() {
        return (root, query, cb) ->
                cb.isTrue(root.get("category").get("active"));
    }

    private static Specification<ProductEntity> inCategory(ProductFilter filter) {
        return (root, query, cb) ->
                filter.hasCategory()
                        ? cb.equal(root.get("category").get("id"), filter.getCategoryId())
                        : cb.conjunction();
    }

    private static Specification<ProductEntity> nameContains(ProductFilter filter) {
        return (root, query, cb) ->
                filter.hasKeyword()
                        ? cb.like(cb.lower(root.get("name")), "%" + filter.getKeyword().toLowerCase() + "%")
                        : cb.conjunction();
    }
}