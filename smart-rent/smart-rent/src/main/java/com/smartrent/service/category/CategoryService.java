package com.smartrent.service.category;

import com.smartrent.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
  List<CategoryResponse> getAllCategories();
  List<CategoryResponse> getActiveCategories();
  CategoryResponse getCategoryById(Long categoryId);
}
