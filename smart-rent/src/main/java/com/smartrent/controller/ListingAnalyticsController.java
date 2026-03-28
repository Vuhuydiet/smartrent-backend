package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingAnalyticsResponse;
import com.smartrent.dto.response.OwnerListingsAnalyticsResponse;
import com.smartrent.dto.response.OwnerSavedListingsAnalyticsResponse;
import com.smartrent.dto.response.SavedListingsTrendResponse;
import com.smartrent.service.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/owners/listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Owner Listing Analytics", description = "Analytics dashboard APIs for listing owners")
public class ListingAnalyticsController {

    AnalyticsService analyticsService;

    /**
     * Resolves the `period` query param to a "since" LocalDateTime.
     * Supported values: 7d, 30d, 90d, 180d, 365d, all (or null = 30d default).
     */
    private LocalDateTime resolveSince(String period) {
        if (period == null || period.isBlank()) {
            period = "30d"; // default
        }
        if ("all".equalsIgnoreCase(period)) {
            return null; // no filtering
        }
        int days = switch (period.toLowerCase()) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            case "180d" -> 180;
            case "365d" -> 365;
            default -> 30;
        };
        return LocalDateTime.now().minusDays(days);
    }

    @GetMapping("/{listingId}/analytics")
    @Operation(
            summary = "Get analytics for a specific listing",
            description = "Returns analytics for a listing including clicks, views, conversion rate, and time-series data, " +
                    "filtered by period (7d, 30d, 90d, 180d, 365d, all). Defaults to last 30 days.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Analytics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "listingId": 123,
                                        "listingTitle": "Beautiful 2BR Apartment",
                                        "totalClicks": 45,
                                        "totalViews": 320,
                                        "conversionRate": 0.1406,
                                        "clicksOverTime": [
                                          {"date": "2026-03-14", "count": 5},
                                          {"date": "2026-03-15", "count": 8}
                                        ],
                                        "clicksByDayOfWeek": {
                                          "MON": 10, "TUE": 8, "WED": 6, "THU": 5,
                                          "FRI": 7, "SAT": 5, "SUN": 4
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the listing owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ApiResponse<ListingAnalyticsResponse> getListingAnalytics(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId,

            @Parameter(description = "Time period filter: 7d, 30d, 90d, 180d, 365d, or all. Defaults to 30d.", example = "30d")
            @RequestParam(required = false) String period
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();
        LocalDateTime since = resolveSince(period);

        log.info("Owner {} requesting analytics for listing {} (period={})", ownerId, listingId, period);

        ListingAnalyticsResponse response = analyticsService.getListingAnalytics(listingId, ownerId, since);

        return ApiResponse.<ListingAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/analytics")
    @Operation(
            summary = "Get paginated analytics summary for all owner's listings",
            description = "Returns click summaries per listing for the authenticated owner with pagination.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Analytics summary retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "listings": [
                                          {"listingId": 123, "listingTitle": "2BR Apartment", "totalClicks": 45},
                                          {"listingId": 456, "listingTitle": "Studio Near Park", "totalClicks": 12}
                                        ],
                                        "currentPage": 0,
                                        "totalPages": 5,
                                        "totalElements": 48,
                                        "pageSize": 10
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<OwnerListingsAnalyticsResponse> getOwnerListingsAnalytics(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);

        log.info("Owner {} requesting analytics page={}, size={}", ownerId, page, size);

        OwnerListingsAnalyticsResponse response = analyticsService.getOwnerListingsAnalytics(ownerId, pageable);

        return ApiResponse.<OwnerListingsAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/saves-trend")
    @Operation(
            summary = "Get saved-listing trend for a specific listing",
            description = "Returns the total save count and daily save trend, " +
                    "filtered by period (7d, 30d, 90d, 180d, 365d, all). Defaults to last 30 days.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Saved listings trend retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "listingId": 123,
                                        "listingTitle": "Beautiful 2BR Apartment",
                                        "totalSaves": 28,
                                        "savesOverTime": [
                                          {"date": "2026-03-14", "count": 3},
                                          {"date": "2026-03-15", "count": 5},
                                          {"date": "2026-03-16", "count": 2}
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the listing owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ApiResponse<SavedListingsTrendResponse> getSavedListingTrend(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId,

            @Parameter(description = "Time period filter: 7d, 30d, 90d, 180d, 365d, or all. Defaults to 30d.", example = "30d")
            @RequestParam(required = false) String period
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();
        LocalDateTime since = resolveSince(period);

        log.info("Owner {} requesting saves trend for listing {} (period={})", ownerId, listingId, period);

        SavedListingsTrendResponse response = analyticsService.getSavedListingTrend(listingId, ownerId, since);

        return ApiResponse.<SavedListingsTrendResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/saves-analytics")
    @Operation(
            summary = "Get paginated saves summary for all owner's listings",
            description = "Returns save counts per listing for the authenticated owner with pagination, sorted by most saved.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Saves analytics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "listings": [
                                          {"listingId": 123, "listingTitle": "2BR Apartment", "totalSaves": 28},
                                          {"listingId": 456, "listingTitle": "Studio Near Park", "totalSaves": 9}
                                        ],
                                        "totalSavesAcrossAll": 37,
                                        "currentPage": 0,
                                        "totalPages": 3,
                                        "totalElements": 25,
                                        "pageSize": 10
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<OwnerSavedListingsAnalyticsResponse> getOwnerSavedListingsAnalytics(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);

        log.info("Owner {} requesting saves analytics page={}, size={}", ownerId, page, size);

        OwnerSavedListingsAnalyticsResponse response = analyticsService.getOwnerSavedListingsAnalytics(ownerId, pageable);

        return ApiResponse.<OwnerSavedListingsAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }
}
