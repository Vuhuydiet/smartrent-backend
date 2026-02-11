package com.smartrent.mapper.impl;

import com.smartrent.dto.response.CategoryResponse;
import com.smartrent.infra.repository.entity.Category;
import com.smartrent.mapper.CategoryMapper;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapperImpl implements CategoryMapper {

  @Override
  public CategoryResponse mapFromCategoryEntityToCategoryResponse(Category category) {
    return CategoryResponse.builder()
        .categoryId(category.getCategoryId())
        .name(category.getName())
        .slug(category.getSlug())
        .description(category.getDescription())
        .icon(category.getIcon())
        .isActive(category.getIsActive())
        .createdAt(category.getCreatedAt())
        .updatedAt(category.getUpdatedAt())
        .build();
  }
}
