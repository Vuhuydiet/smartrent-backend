package com.smartrent.service.category.impl;

import com.smartrent.dto.response.CategoryResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.CategoryRepository;
import com.smartrent.infra.repository.entity.Category;
import com.smartrent.mapper.CategoryMapper;
import com.smartrent.service.category.CategoryService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

  CategoryRepository categoryRepository;
  CategoryMapper categoryMapper;

  @Override
  public List<CategoryResponse> getAllCategories() {
    log.info("Fetching all categories from database");

    try {
      List<Category> categories = categoryRepository.findAll();
      log.info("Successfully retrieved {} categories", categories.size());

      List<CategoryResponse> response = categories.stream()
          .map(categoryMapper::mapFromCategoryEntityToCategoryResponse)
          .collect(Collectors.toList());

      log.debug("Mapped {} category entities to response DTOs", response.size());
      return response;

    } catch (Exception e) {
      log.error("Failed to retrieve categories from database", e);
      throw e;
    }
  }

  @Override
  public List<CategoryResponse> getActiveCategories() {
    log.info("Fetching active categories from database");

    try {
      List<Category> categories = categoryRepository.findAll().stream()
          .filter(Category::getIsActive)
          .collect(Collectors.toList());

      log.info("Successfully retrieved {} active categories", categories.size());

      List<CategoryResponse> response = categories.stream()
          .map(categoryMapper::mapFromCategoryEntityToCategoryResponse)
          .collect(Collectors.toList());

      log.debug("Mapped {} active category entities to response DTOs", response.size());
      return response;

    } catch (Exception e) {
      log.error("Failed to retrieve active categories from database", e);
      throw e;
    }
  }

  @Override
  public CategoryResponse getCategoryById(Long categoryId) {
    log.info("Fetching category with ID: {}", categoryId);

    try {
      Category category = categoryRepository.findById(categoryId)
          .orElseThrow(() -> {
            log.error("Category not found with ID: {}", categoryId);
            return new AppException(DomainCode.CATEGORY_NOT_FOUND);
          });

      log.info("Successfully retrieved category with ID: {}", categoryId);
      return categoryMapper.mapFromCategoryEntityToCategoryResponse(category);

    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to retrieve category with ID: {}", categoryId, e);
      throw e;
    }
  }
}
