package com.smartrent.dto.response;

import com.smartrent.enums.NewsCategory;
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
@Schema(description = "News summary for list view (without full content)")
public class NewsSummaryResponse {

    @Schema(description = "News ID", example = "1")
    Long newsId;

    @Schema(description = "News title", example = "Top 10 Tips for Finding the Perfect Rental")
    String title;

    @Schema(description = "URL-friendly slug", example = "top-10-tips-for-finding-the-perfect-rental")
    String slug;

    @Schema(description = "Brief summary")
    String summary;

    @Schema(description = "News category", example = "BLOG")
    NewsCategory category;

    @Schema(description = "List of tags")
    List<String> tags;

    @Schema(description = "Thumbnail image URL")
    String thumbnailUrl;

    @Schema(description = "Publication date and time")
    LocalDateTime publishedAt;

    @Schema(description = "Author display name")
    String authorName;

    @Schema(description = "Number of views", example = "1250")
    Long viewCount;

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt;
}

