package com.techora.catalog.projection.repository;

import com.techora.catalog.projection.entity.ProductReadModelEntity;
import com.techora.catalog.domain.valueobject.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductReadModelRepository extends JpaRepository<ProductReadModelEntity, UUID> {

    @Query("""
            select product from ProductReadModelEntity product
            where product.status = :status
              and product.categoryActive = true
              and (:categoryId is null or product.categoryId = :categoryId)
            """)
    Page<ProductReadModelEntity> searchPublicProducts(@Param("status") ProductStatus status,
                                                      @Param("categoryId") UUID categoryId,
                                                      Pageable pageable);

    @Query("""
            select product from ProductReadModelEntity product
            where product.status = :status
              and product.categoryActive = true
              and (:categoryId is null or product.categoryId = :categoryId)
              and lower(product.name) like lower(concat('%', :keyword, '%'))
            """)
    Page<ProductReadModelEntity> searchPublicProductsByKeyword(@Param("status") ProductStatus status,
                                                               @Param("categoryId") UUID categoryId,
                                                               @Param("keyword") String keyword,
                                                               Pageable pageable);

    Optional<ProductReadModelEntity> findBySlugAndStatusAndCategoryActiveTrue(
            String slug,
            ProductStatus status
    );
}
