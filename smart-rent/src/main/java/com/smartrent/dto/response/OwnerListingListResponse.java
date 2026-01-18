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
 * Paginated listing response for owner with owner-specific information
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Paginated listing response for owner with owner-specific details")
public class OwnerListingListResponse {

    @Schema(description = "List of owner's listings with detailed owner information")
    List<ListingResponseForOwner> listings;

    @Schema(description = "Total number of listings matching the filter criteria", example = "25")
    Long totalCount;

    @Schema(description = "Current page number (one-based)", example = "1")
    Integer currentPage;

    @Schema(description = "Page size", example = "20")
    Integer pageSize;

    @Schema(description = "Total number of pages", example = "2")
    Integer totalPages;

    @Schema(description = "Filter criteria used for this query")
    Object filterCriteria;

    @Schema(description = "Statistics summary for owner's listings")
    OwnerStatistics statistics;

    /**
     * Statistics summary for owner view
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Statistics summary for owner's listings")
    public static class OwnerStatistics {
        @Schema(description = "Total number of draft listings", example = "3")
        Long drafts;

        @Schema(description = "Total number of listings pending verification", example = "5")
        Long pendingVerification;

        @Schema(description = "Total number of rejected listings", example = "2")
        Long rejected;

        @Schema(description = "Total number of verified/active listings", example = "12")
        Long active;

        @Schema(description = "Total number of expired listings", example = "5")
        Long expired;

        @Schema(description = "Total number of NORMAL tier listings", example = "15")
        Long normalListings;

        @Schema(description = "Total number of SILVER tier listings", example = "5")
        Long silverListings;

        @Schema(description = "Total number of GOLD tier listings", example = "3")
        Long goldListings;

        @Schema(description = "Total number of DIAMOND tier listings", example = "2")
        Long diamondListings;
    }
}