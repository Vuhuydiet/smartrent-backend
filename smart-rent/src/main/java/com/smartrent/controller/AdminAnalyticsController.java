package com.smartrent.controller;

import com.smartrent.dto.response.AdminListingAnalyticsResponse;
import com.smartrent.dto.response.AdminReportAnalyticsResponse;
import com.smartrent.dto.response.AdminUserAnalyticsResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;
import com.smartrent.service.admin.analytics.AdminAnalyticsService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

@RestController
@RequestMapping("/v1/admin/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Analytics", description = "Admin analytics chart data APIs")
public class AdminAnalyticsController {

    AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/revenue")
    @Operation(
            summary = "Get revenue over time",
            description = """
                    Returns revenue data points, grand total, and breakdown by transaction type.
                    Use `days` for preset ranges (7, 30, 360) with automatic granularity,
                    or `from`/`to` for custom date ranges (always daily granularity).
                    When `days` is provided, `from`/`to` are ignored.""",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Revenue data retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "dataPoints": [
                                          {"date": "2026-03-01", "totalAmount": 2800000, "transactionCount": 2},
                                          {"date": "2026-03-02", "totalAmount": 1400000, "transactionCount": 1}
                                        ],
                                        "grandTotal": 4200000,
                                        "totalTransactions": 3,
                                        "revenueByType": [
                                          {"transactionType": "MEMBERSHIP_PURCHASE", "totalAmount": 2800000, "transactionCount": 2}
                                        ],
                                        "granularity": "DAY"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<RevenueOverTimeResponse> getRevenueOverTime(
            @Parameter(description = "Preset range in days (7, 30, 360). Takes priority over from/to.", example = "7")
            @RequestParam(required = false) Integer days,

            @Parameter(description = "Start date (inclusive), defaults to 30 days ago", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-03-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        RevenueOverTimeResponse response;

        if (days != null) {
            log.info("Admin requesting revenue data for last {} days", days);
            response = adminAnalyticsService.getRevenueOverTime(days);
        } else {
            LocalDate endDate = (to != null) ? to : LocalDate.now();
            LocalDate startDate = (from != null) ? from : endDate.minusDays(30);
            log.info("Admin requesting revenue data from {} to {}", startDate, endDate);
            response = adminAnalyticsService.getRevenueOverTime(startDate, endDate);
        }

        return ApiResponse.<RevenueOverTimeResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/memberships/distribution")
    @Operation(
            summary = "Get active membership distribution by package level",
            description = "Returns the count and percentage of currently active memberships grouped by package level.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Membership distribution retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "distribution": [
                                          {"packageLevel": "BASIC", "packageName": "Gói Cơ Bản 1 Tháng", "count": 45, "percentage": 56.25},
                                          {"packageLevel": "STANDARD", "packageName": "Gói Tiêu Chuẩn 1 Tháng", "count": 25, "percentage": 31.25},
                                          {"packageLevel": "ADVANCED", "packageName": "Gói Nâng Cao 1 Tháng", "count": 10, "percentage": 12.50}
                                        ],
                                        "totalActive": 80
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<MembershipDistributionResponse> getMembershipDistribution() {
        log.info("Admin requesting membership distribution");

        MembershipDistributionResponse response = adminAnalyticsService.getMembershipDistribution();

        return ApiResponse.<MembershipDistributionResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    // ─── Analytics Charts ───

    @GetMapping("/users")
    @Operation(
            summary = "Get user growth analytics",
            description = """
                    Returns new-registration time series, a cumulative user-growth curve, and breakdowns
                    by role (regular vs broker) and by broker verification status.
                    Use `days` for preset ranges (7, 30, 360) with automatic granularity (day for 7/30, month for 360),
                    or `from`/`to` for custom date ranges (always daily granularity).
                    When `days` is provided, `from`/`to` are ignored. If no parameter is given, defaults to last 7 days.""",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User analytics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "dataPoints": [
                                          {"label": "2026-03-23", "count": 5},
                                          {"label": "2026-03-24", "count": 12},
                                          {"label": "2026-03-25", "count": 8}
                                        ],
                                        "total": 25,
                                        "granularity": "DAY",
                                        "cumulativeDataPoints": [
                                          {"label": "2026-03-23", "count": 1005},
                                          {"label": "2026-03-24", "count": 1017},
                                          {"label": "2026-03-25", "count": 1025}
                                        ],
                                        "totalUsersAsOfRangeEnd": 1025,
                                        "roleBreakdown": [
                                          {"category": "REGULAR", "count": 20, "percentage": 80.0},
                                          {"category": "BROKER", "count": 5, "percentage": 20.0}
                                        ],
                                        "brokerVerificationBreakdown": [
                                          {"category": "PENDING", "count": 3, "percentage": 60.0},
                                          {"category": "APPROVED", "count": 2, "percentage": 40.0}
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<AdminUserAnalyticsResponse> getUserAnalytics(
            @Parameter(description = "Preset range in days (7, 30, or 360). Takes priority over from/to.", example = "7")
            @RequestParam(required = false) Integer days,

            @Parameter(description = "Start date (inclusive), defaults to 7 days ago when from/to is used", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-03-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.<AdminUserAnalyticsResponse>builder()
                .code("999999")
                .data(resolveAnalytics(days, from, to,
                        adminAnalyticsService::getUserAnalytics,
                        adminAnalyticsService::getUserAnalytics,
                        "user analytics"))
                .build();
    }

    @GetMapping("/reports")
    @Operation(
            summary = "Get listing report analytics",
            description = """
                    Returns report-count time series, a cumulative report curve, breakdowns by category
                    (LISTING/MAP) and status (PENDING/RESOLVED/REJECTED), plus resolution rate and average
                    resolution time.
                    Use `days` for preset ranges (7, 30, 360) with automatic granularity (day for 7/30, month for 360),
                    or `from`/`to` for custom date ranges (always daily granularity).
                    When `days` is provided, `from`/`to` are ignored. If no parameter is given, defaults to last 7 days.""",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report analytics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "dataPoints": [
                                          {"label": "2026-03-23", "count": 2},
                                          {"label": "2026-03-24", "count": 0},
                                          {"label": "2026-03-25", "count": 5}
                                        ],
                                        "total": 7,
                                        "granularity": "DAY",
                                        "cumulativeDataPoints": [
                                          {"label": "2026-03-23", "count": 102},
                                          {"label": "2026-03-24", "count": 102},
                                          {"label": "2026-03-25", "count": 107}
                                        ],
                                        "categoryBreakdown": [
                                          {"category": "LISTING", "count": 5, "percentage": 71.43},
                                          {"category": "MAP", "count": 2, "percentage": 28.57}
                                        ],
                                        "statusBreakdown": [
                                          {"category": "PENDING", "count": 4, "percentage": 57.14},
                                          {"category": "RESOLVED", "count": 2, "percentage": 28.57},
                                          {"category": "REJECTED", "count": 1, "percentage": 14.29}
                                        ],
                                        "resolutionRatePercent": 42.86,
                                        "avgResolutionHours": 5.75
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<AdminReportAnalyticsResponse> getReportAnalytics(
            @Parameter(description = "Preset range in days (7, 30, or 360). Takes priority over from/to.", example = "7")
            @RequestParam(required = false) Integer days,

            @Parameter(description = "Start date (inclusive), defaults to 7 days ago when from/to is used", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-03-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.<AdminReportAnalyticsResponse>builder()
                .code("999999")
                .data(resolveAnalytics(days, from, to,
                        adminAnalyticsService::getReportAnalytics,
                        adminAnalyticsService::getReportAnalytics,
                        "report analytics"))
                .build();
    }

    @GetMapping("/listings")
    @Operation(
            summary = "Get listing creation analytics",
            description = """
                    Returns listing-creation time series (excluding drafts and shadows), a cumulative
                    listings curve, and breakdowns by listing type (RENT/SALE/SHARE), product type
                    (ROOM/APARTMENT/HOUSE/OFFICE/STUDIO/STORE), and verification status.
                    Use `days` for preset ranges (7, 30, 360) with automatic granularity (day for 7/30, month for 360),
                    or `from`/`to` for custom date ranges (always daily granularity).
                    When `days` is provided, `from`/`to` are ignored. If no parameter is given, defaults to last 7 days.""",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing analytics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "999999",
                                      "data": {
                                        "dataPoints": [
                                          {"label": "2026-03-23", "count": 10},
                                          {"label": "2026-03-24", "count": 15},
                                          {"label": "2026-03-25", "count": 7}
                                        ],
                                        "total": 32,
                                        "granularity": "DAY",
                                        "cumulativeDataPoints": [
                                          {"label": "2026-03-23", "count": 4210},
                                          {"label": "2026-03-24", "count": 4225},
                                          {"label": "2026-03-25", "count": 4232}
                                        ],
                                        "totalListingsAsOfRangeEnd": 4232,
                                        "listingTypeBreakdown": [
                                          {"category": "RENT", "count": 24, "percentage": 75.0},
                                          {"category": "SALE", "count": 6, "percentage": 18.75},
                                          {"category": "SHARE", "count": 2, "percentage": 6.25}
                                        ],
                                        "productTypeBreakdown": [
                                          {"category": "ROOM", "count": 18, "percentage": 56.25},
                                          {"category": "APARTMENT", "count": 10, "percentage": 31.25},
                                          {"category": "HOUSE", "count": 4, "percentage": 12.5}
                                        ],
                                        "verificationBreakdown": [
                                          {"category": "VERIFIED", "count": 20, "percentage": 62.5},
                                          {"category": "UNVERIFIED", "count": 12, "percentage": 37.5}
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<AdminListingAnalyticsResponse> getListingAnalytics(
            @Parameter(description = "Preset range in days (7, 30, or 360). Takes priority over from/to.", example = "7")
            @RequestParam(required = false) Integer days,

            @Parameter(description = "Start date (inclusive), defaults to 7 days ago when from/to is used", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-03-29")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.<AdminListingAnalyticsResponse>builder()
                .code("999999")
                .data(resolveAnalytics(days, from, to,
                        adminAnalyticsService::getListingAnalytics,
                        adminAnalyticsService::getListingAnalytics,
                        "listing analytics"))
                .build();
    }

    private <T> T resolveAnalytics(
            Integer days,
            LocalDate from,
            LocalDate to,
            IntFunction<T> byDays,
            BiFunction<LocalDate, LocalDate, T> byRange,
            String label
    ) {
        if (days != null) {
            log.info("Admin requesting {} for last {} days", label, days);
            return byDays.apply(days);
        }
        if (from != null || to != null) {
            LocalDate endDate = (to != null) ? to : LocalDate.now();
            LocalDate startDate = (from != null) ? from : endDate.minusDays(7);
            log.info("Admin requesting {} from {} to {}", label, startDate, endDate);
            return byRange.apply(startDate, endDate);
        }
        log.info("Admin requesting {} for default last 7 days", label);
        return byDays.apply(7);
    }
}
