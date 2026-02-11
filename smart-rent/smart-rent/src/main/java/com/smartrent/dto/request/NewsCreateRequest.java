package com.smartrent.dto.request;

import com.smartrent.enums.NewsCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create a new news/blog post")
public class NewsCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "News title", example = "Top 10 Tips for Finding the Perfect Rental")
    String title;

    @Size(max = 1000, message = "Summary must not exceed 1000 characters")
    @Schema(description = "Brief summary of the news", example = "Discover the best strategies for finding your ideal rental property in 2024")
    String summary;

    @NotBlank(message = "Content is required")
    @Schema(description = "Full content in HTML or Markdown format")
    String content;

    @NotNull(message = "Category is required")
    @Schema(description = "News category", example = "BLOG")
    NewsCategory category;

    @Schema(description = "Comma-separated tags", example = "rental,tips,guide,housing")
    String tags;

    @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
    @Schema(description = "URL of the thumbnail image")
    String thumbnailUrl;

    @Size(max = 255, message = "Meta title must not exceed 255 characters")
    @Schema(description = "SEO meta title")
    String metaTitle;

    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    @Schema(description = "SEO meta description")
    String metaDescription;

    @Size(max = 500, message = "Meta keywords must not exceed 500 characters")
    @Schema(description = "SEO keywords")
    String metaKeywords;
}

