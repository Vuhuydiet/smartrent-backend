package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.CategoryResponse;
import com.smartrent.service.category.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category Management", description = "APIs for managing property listing categories")
public class CategoryController {

  CategoryService categoryService;

  @GetMapping
  @Operation(
      summary = "Get all categories",
      description = "Retrieves a list of all available categories in the system, including both active and inactive categories. This endpoint is public and does not require authentication."
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Categories retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": [
                          {
                            "categoryId": 1,
                            "name": "Cho thuê phòng trọ",
                            "slug": "cho-thue-phong-tro",
                            "description": "Phòng trọ giá rẻ, phòng trọ sinh viên",
                            "icon": "room",
                            "isActive": true,
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-20T14:45:00"
                          },
                          {
                            "categoryId": 2,
                            "name": "Cho thuê căn hộ",
                            "slug": "cho-thue-can-ho",
                            "description": "Căn hộ chung cư, căn hộ dịch vụ",
                            "icon": "apartment",
                            "isActive": true,
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-20T14:45:00"
                          }
                        ]
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "INTERNAL_SERVER_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<List<CategoryResponse>> getAllCategories() {
    List<CategoryResponse> categories = categoryService.getAllCategories();

    return ApiResponse.<List<CategoryResponse>>builder()
        .data(categories)
        .build();
  }

  @GetMapping("/active")
  @Operation(
      summary = "Get active categories",
      description = "Retrieves a list of only active categories. This endpoint is public and does not require authentication. It is typically used for public-facing interfaces where users need to select a category for their listings."
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Active categories retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": [
                          {
                            "categoryId": 1,
                            "name": "Cho thuê phòng trọ",
                            "slug": "cho-thue-phong-tro",
                            "description": "Phòng trọ giá rẻ, phòng trọ sinh viên",
                            "icon": "room",
                            "isActive": true,
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-20T14:45:00"
                          }
                        ]
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "INTERNAL_SERVER_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<List<CategoryResponse>> getActiveCategories() {
    List<CategoryResponse> categories = categoryService.getActiveCategories();

    return ApiResponse.<List<CategoryResponse>>builder()
        .data(categories)
        .build();
  }

  @GetMapping("/{categoryId}")
  @Operation(
      summary = "Get category by ID",
      description = "Retrieves detailed information about a specific category by its unique identifier. This endpoint is public and does not require authentication."
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Category retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "categoryId": 1,
                          "name": "Cho thuê phòng trọ",
                          "slug": "cho-thue-phong-tro",
                          "description": "Phòng trọ giá rẻ, phòng trọ sinh viên",
                          "icon": "room",
                          "isActive": true,
                          "createdAt": "2024-01-15T10:30:00",
                          "updatedAt": "2024-01-20T14:45:00"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "Category not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "404001",
                        "message": "CATEGORY_NOT_FOUND",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "INTERNAL_SERVER_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  public ApiResponse<CategoryResponse> getCategoryById(
      @Parameter(description = "Unique identifier of the category", required = true, example = "1")
      @PathVariable Long categoryId) {
    CategoryResponse category = categoryService.getCategoryById(categoryId);

    return ApiResponse.<CategoryResponse>builder()
        .data(category)
        .build();
  }
}
