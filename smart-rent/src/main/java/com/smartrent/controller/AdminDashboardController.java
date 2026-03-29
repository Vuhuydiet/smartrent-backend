package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;
import com.smartrent.dto.response.TimeSeriesResponse;
import com.smartrent.service.dashboard.AdminDashboardService;
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

@RestController
@RequestMapping("/v1/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Dashboard", description = "Admin dashboard chart data APIs")
public class AdminDashboardController {

    AdminDashboardService adminDashboardService;

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
            response = adminDashboardService.getRevenueOverTime(days);
        } else {
            LocalDate endDate = (to != null) ? to : LocalDate.now();
            LocalDate startDate = (from != null) ? from : endDate.minusDays(30);
            log.info("Admin requesting revenue data from {} to {}", startDate, endDate);
            response = adminDashboardService.getRevenueOverTime(startDate, endDate);
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

        MembershipDistributionResponse response = adminDashboardService.getMembershipDistribution();

        return ApiResponse.<MembershipDistributionResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    // ─── New Analytics Charts ───

    @GetMapping("/users/growth")
    @Operation(
            summary = "Get user growth over time",
            description = "Returns count of new user registrations grouped by day (7/30 days) or month (360 days).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User growth data retrieved successfully",
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
                                        "granularity": "DAY"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<TimeSeriesResponse> getUserGrowth(
            @Parameter(description = "Time range in days (7, 30, or 360)", example = "7")
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("Admin requesting user growth for last {} days", days);
        return ApiResponse.<TimeSeriesResponse>builder()
                .code("999999")
                .data(adminDashboardService.getUserGrowth(days))
                .build();
    }

    @GetMapping("/reports/count")
    @Operation(
            summary = "Get report count over time",
            description = "Returns count of listing reports created, grouped by day (7/30 days) or month (360 days).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report count data retrieved successfully",
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
                                        "granularity": "DAY"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<TimeSeriesResponse> getReportCount(
            @Parameter(description = "Time range in days (7, 30, or 360)", example = "7")
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("Admin requesting report count for last {} days", days);
        return ApiResponse.<TimeSeriesResponse>builder()
                .code("999999")
                .data(adminDashboardService.getReportCount(days))
                .build();
    }

    @GetMapping("/listings/creation")
    @Operation(
            summary = "Get listing creation over time",
            description = "Returns count of new listings created (excluding drafts and shadows), grouped by day (7/30 days) or month (360 days).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing creation data retrieved successfully",
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
                                        "granularity": "DAY"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<TimeSeriesResponse> getListingCreation(
            @Parameter(description = "Time range in days (7, 30, or 360)", example = "7")
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("Admin requesting listing creation for last {} days", days);
        return ApiResponse.<TimeSeriesResponse>builder()
                .code("999999")
                .data(adminDashboardService.getListingCreation(days))
                .build();
    }
}
