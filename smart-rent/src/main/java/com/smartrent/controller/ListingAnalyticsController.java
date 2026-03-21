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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/owners/listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Owner Listing Analytics", description = "Analytics dashboard APIs for listing owners")
public class ListingAnalyticsController {

    AnalyticsService analyticsService;

    @GetMapping("/{listingId}/analytics")
    @Operation(
            summary = "Get analytics for a specific listing",
            description = "Returns full analytics for a listing including clicks, views, conversion rate, and time-series data. Only the listing owner can access this.",
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
            @PathVariable Long listingId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();

        log.info("Owner {} requesting analytics for listing {}", ownerId, listingId);

        ListingAnalyticsResponse response = analyticsService.getListingAnalytics(listingId, ownerId);

        return ApiResponse.<ListingAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/analytics")
    @Operation(
            summary = "Get analytics summary for all owner's listings",
            description = "Returns click summaries per listing for the authenticated owner.",
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
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<OwnerListingsAnalyticsResponse> getOwnerListingsAnalytics() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();

        log.info("Owner {} requesting analytics for all listings", ownerId);

        OwnerListingsAnalyticsResponse response = analyticsService.getOwnerListingsAnalytics(ownerId);

        return ApiResponse.<OwnerListingsAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/saves-trend")
    @Operation(
            summary = "Get saved-listing trend for a specific listing",
            description = "Returns the total save count and daily save trend for a listing. Only the listing owner can access this.",
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
                                          {"date": "2026-03-16", "count": 2},
                                          {"date": "2026-03-17", "count": 8},
                                          {"date": "2026-03-18", "count": 4},
                                          {"date": "2026-03-19", "count": 3},
                                          {"date": "2026-03-20", "count": 3}
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
            @PathVariable Long listingId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();

        log.info("Owner {} requesting saves trend for listing {}", ownerId, listingId);

        SavedListingsTrendResponse response = analyticsService.getSavedListingTrend(listingId, ownerId);

        return ApiResponse.<SavedListingsTrendResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/saves-analytics")
    @Operation(
            summary = "Get saves summary for all owner's listings",
            description = "Returns save counts per listing for the authenticated owner, sorted by most saved.",
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
                                        "totalSavesAcrossAll": 37
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ApiResponse<OwnerSavedListingsAnalyticsResponse> getOwnerSavedListingsAnalytics() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String ownerId = authentication.getName();

        log.info("Owner {} requesting saves analytics for all listings", ownerId);

        OwnerSavedListingsAnalyticsResponse response = analyticsService.getOwnerSavedListingsAnalytics(ownerId);

        return ApiResponse.<OwnerSavedListingsAnalyticsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }
}
