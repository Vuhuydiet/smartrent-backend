package com.smartrent.dto.request;

import com.smartrent.enums.NewsCategory;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update an existing news/blog post")
public class NewsUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "News title")
    String title;

    @Size(max = 1000, message = "Summary must not exceed 1000 characters")
    @Schema(description = "Brief summary of the news")
    String summary;

    @Schema(description = "Full content in HTML or Markdown format")
    String content;

    @Schema(description = "News category")
    NewsCategory category;

    @Schema(description = "Comma-separated tags")
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

