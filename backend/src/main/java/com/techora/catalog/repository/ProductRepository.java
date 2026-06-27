package com.techora.catalog.repository;

import com.techora.catalog.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>,
        JpaSpecificationExecutor<ProductEntity> {

    boolean existsBySkuIgnoreCaseOrSlugAndIdNot(String sku, String slug, UUID id);

    boolean existsBySkuIgnoreCaseOrSlug(String sku, String slug);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findWithCategoryById(UUID id);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findWithCategoryByIdIn(Collection<UUID> ids);

    @Query("select product from ProductEntity product join fetch product.category where product.category.id = :categoryId")
    List<ProductEntity> findWithCategoryByCategoryId(@Param("categoryId") UUID categoryId);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllBy();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductEntity product join fetch product.category where product.id = :id")
    Optional<ProductEntity> findLockedWithCategoryById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findWithCategoryBySlug(String slug);

}
