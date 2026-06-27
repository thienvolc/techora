package com.techora.catalog.mapper;

import com.techora.catalog.dto.request.CategoryRequest;
import com.techora.catalog.dto.response.CategoryResponse;
import com.techora.catalog.entity.CategoryEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CategoryMapper {

    public CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(
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
