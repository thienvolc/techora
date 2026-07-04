package com.techora.catalog.persistence.mapper;

import com.techora.catalog.application.model.CategoryView;
import com.techora.catalog.controller.request.CategoryRequest;
import com.techora.catalog.domain.entity.CategoryEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CategoryMapper {

    public CategoryView toView(CategoryEntity entity) {
        return new CategoryView(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CategoryEntity toEntity(CategoryRequest request, String slug) {
        Instant now = Instant.now();
        return CategoryEntity.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .active(request.active())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
