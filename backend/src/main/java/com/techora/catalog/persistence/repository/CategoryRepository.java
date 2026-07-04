package com.techora.catalog.persistence.repository;

import com.techora.catalog.domain.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCaseOrSlug(String name, String slug);

    boolean existsByNameIgnoreCaseOrSlugAndIdNot(String name, String slug, UUID id);
}
