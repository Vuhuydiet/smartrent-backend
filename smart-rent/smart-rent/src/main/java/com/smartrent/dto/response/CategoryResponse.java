package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object containing category information")
public class CategoryResponse {

  @Schema(
      description = "Unique identifier for the category",
      example = "1"
  )
  Long categoryId;

  @Schema(
      description = "Name of the category",
      example = "Cho thuê phòng trọ"
  )
  String name;

  @Schema(
      description = "URL-friendly slug for the category",
      example = "cho-thue-phong-tro"
  )
  String slug;

  @Schema(
      description = "Detailed description of the category",
      example = "Phòng trọ giá rẻ, phòng trọ sinh viên"
  )
  String description;

  @Schema(
      description = "Icon identifier for the category",
      example = "room"
  )
  String icon;

  @Schema(
      description = "Whether the category is currently active",
      example = "true"
  )
  Boolean isActive;

  @Schema(
      description = "Timestamp when the category was created",
      example = "2024-01-15T10:30:00"
  )
  LocalDateTime createdAt;

  @Schema(
      description = "Timestamp when the category was last updated",
      example = "2024-01-20T14:45:00"
  )
  LocalDateTime updatedAt;
}
