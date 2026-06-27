package com.techora.catalog.repository;

import com.techora.catalog.entity.ProductReadModelEntity;
import com.techora.catalog.entity.ProductStatus;
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
              and (:keyword is null or lower(product.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<ProductReadModelEntity> searchPublicProducts(@Param("status") ProductStatus status,
                                                      @Param("categoryId") UUID categoryId,
                                                      @Param("keyword") String keyword,
                                                      Pageable pageable);

    Optional<ProductReadModelEntity> findBySlugAndStatusAndCategoryActiveTrue(
            String slug,
            ProductStatus status
    );
}
