package com.smartrent.dto.response;

import com.smartrent.enums.NewsCategory;
import com.smartrent.enums.NewsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "News/blog post response")
public class NewsResponse {

    @Schema(description = "News ID", example = "1")
    Long newsId;

    @Schema(description = "News title", example = "Top 10 Tips for Finding the Perfect Rental")
    String title;

    @Schema(description = "URL-friendly slug", example = "top-10-tips-for-finding-the-perfect-rental")
    String slug;

    @Schema(description = "Brief summary")
    String summary;

    @Schema(description = "Full content (HTML or Markdown)")
    String content;

    @Schema(description = "News category", example = "BLOG")
    NewsCategory category;

    @Schema(description = "List of tags")
    List<String> tags;

    @Schema(description = "Thumbnail image URL")
    String thumbnailUrl;

    @Schema(description = "Publication status", example = "PUBLISHED")
    NewsStatus status;

    @Schema(description = "Publication date and time")
    LocalDateTime publishedAt;

    @Schema(description = "Author ID (admin)")
    String authorId;

    @Schema(description = "Author display name")
    String authorName;

    @Schema(description = "Number of views", example = "1250")
    Long viewCount;

    @Schema(description = "SEO meta title")
    String metaTitle;

    @Schema(description = "SEO meta description")
    String metaDescription;

    @Schema(description = "SEO keywords")
    String metaKeywords;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}

