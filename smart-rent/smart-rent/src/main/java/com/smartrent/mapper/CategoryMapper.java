package com.smartrent.mapper;

import com.smartrent.dto.response.CategoryResponse;
import com.smartrent.infra.repository.entity.Category;

public interface CategoryMapper {
  CategoryResponse mapFromCategoryEntityToCategoryResponse(Category category);
}
