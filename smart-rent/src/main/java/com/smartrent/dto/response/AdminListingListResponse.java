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

/**
 * Paginated listing response for admin with admin-specific information
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Paginated listing response for admin with count and filters")
public class AdminListingListResponse {

    @Schema(description = "List of listings for the current page with admin information")
    List<ListingResponseWithAdmin> listings;

    @Schema(description = "Total number of listings matching the filter criteria", example = "150")
    Long totalCount;

    @Schema(description = "Current page number (zero-based)", example = "0")
    Integer currentPage;

    @Schema(description = "Page size", example = "20")
    Integer pageSize;

    @Schema(description = "Total number of pages", example = "8")
    Integer totalPages;

    @Schema(description = "Filter criteria used for this query")
    Object filterCriteria;

    @Schema(description = "Statistics summary for the filtered listings")
    AdminStatistics statistics;

    /**
     * Statistics summary for admin view
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Statistics summary")
    public static class AdminStatistics {
        @Schema(description = "Total number of listings pending verification", example = "45")
        Long pendingVerification;

        @Schema(description = "Total number of verified listings", example = "1250")
        Long verified;

        @Schema(description = "Total number of expired listings", example = "180")
        Long expired;

        @Schema(description = "Total number of draft listings", example = "23")
        Long drafts;

        @Schema(description = "Total number of shadow listings", example = "89")
        Long shadows;

        @Schema(description = "Total number of NORMAL listings", example = "800")
        Long normalListings;

        @Schema(description = "Total number of SILVER listings", example = "200")
        Long silverListings;

        @Schema(description = "Total number of GOLD listings", example = "150")
        Long goldListings;

        @Schema(description = "Total number of DIAMOND listings", example = "100")
        Long diamondListings;
    }
}