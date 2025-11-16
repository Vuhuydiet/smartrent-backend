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
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Paginated listing response with count and recommendations")
public class ListingListResponse {

    @Schema(description = "List of listings for the current page")
    List<ListingResponse> listings;

    @Schema(description = "Total number of listings matching the filter criteria", example = "150")
    Long totalCount;

    @Schema(description = "Current page number (zero-based)", example = "0")
    Integer currentPage;

    @Schema(description = "Page size", example = "20")
    Integer pageSize;

    @Schema(description = "Total number of pages", example = "8")
    Integer totalPages;

    @Schema(description = "Recommended listings based on user preferences and behavior (for future recommendation system)")
    List<ListingResponse> recommendations;

    @Schema(description = "Filter criteria used for this query")
    Object filterCriteria;
}
