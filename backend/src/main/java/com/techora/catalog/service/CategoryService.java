package com.techora.catalog.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.catalog.projection.event.CategoryProjectionChangedEvent;
import com.techora.catalog.dto.request.CategoryRequest;
import com.techora.catalog.dto.response.CategoryResponse;
import com.techora.catalog.entity.CategoryEntity;
import com.techora.catalog.mapper.CategoryMapper;
import com.techora.catalog.repository.CategoryRepository;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.common.infra.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Sort NAME_ASCENDING = Sort.by(Sort.Direction.ASC, "name");

    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final InternalEventPublisher internalEventPublisher;

    @Transactional
    @CacheEvict(cacheNames = CacheNames.ACTIVE_CATEGORIES, allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name();
        String slug = SlugGenerator.generate(name);
        validateUniqueCategoryOrThrow(name, slug);

        CategoryEntity category = mapper.toEntity(request, slug);
        return mapper.toResponse(
                repository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return repository.findAll(NAME_ASCENDING).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.ACTIVE_CATEGORIES)
    public List<CategoryResponse> getActiveCategories() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryEntity getCategoryOrThrow(UUID categoryId) {
        return repository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CATEGORY_NOT_FOUND));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.ACTIVE_CATEGORIES, allEntries = true)
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        String name = request.name();
        String slug = SlugGenerator.generate(name);
        validateUniqueCategoryOrThrow(
                categoryId,
                name,
                slug);

        CategoryEntity category = getCategoryOrThrow(categoryId);
        applyCategoryChanges(
                category,
                request,
                slug);
        CategoryEntity savedCategory = repository.save(category);
        internalEventPublisher.publish(CategoryProjectionChangedEvent.of(savedCategory.getId()));
        return mapper.toResponse(savedCategory);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.ACTIVE_CATEGORIES, allEntries = true)
    public void delete(UUID categoryId) {
        CategoryEntity category = getCategoryOrThrow(categoryId);
        repository.delete(category);
    }

    private void validateUniqueCategoryOrThrow(String name, String slug) {
        if (repository.existsByNameIgnoreCaseOrSlug(name, slug)) {
            throw new BusinessException(ResponseCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private void validateUniqueCategoryOrThrow(UUID categoryId,
                                               String name,
                                               String slug) {

        if (repository.existsByNameIgnoreCaseOrSlugAndIdNot(name, slug, categoryId)) {
            throw new BusinessException(ResponseCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private void applyCategoryChanges(CategoryEntity category,
                                      CategoryRequest request,
                                      String slug) {

        category.setSlug(slug);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setActive(request.active());
        category.markUpdated();
    }

}
