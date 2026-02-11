package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Paginated news list response")
public class NewsListResponse {

    @Schema(description = "List of news items")
    List<NewsSummaryResponse> news;

    @Schema(description = "Total number of items", example = "150")
    Long totalItems;

    @Schema(description = "Current page number (1-based)", example = "1")
    Integer currentPage;

    @Schema(description = "Page size", example = "20")
    Integer pageSize;

    @Schema(description = "Total number of pages", example = "8")
    Integer totalPages;
}

