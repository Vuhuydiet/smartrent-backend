package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.MembershipDistributionResponse;
import com.smartrent.dto.response.RevenueOverTimeResponse;
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
            description = "Returns daily revenue data points, grand total, and revenue breakdown by transaction type for the given date range. Defaults to the last 30 days if no dates provided.",
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
                                          {"date": "2026-03-02", "totalAmount": 1400000, "transactionCount": 1},
                                          {"date": "2026-03-05", "totalAmount": 4200000, "transactionCount": 3}
                                        ],
                                        "grandTotal": 8400000,
                                        "totalTransactions": 6,
                                        "revenueByType": [
                                          {"transactionType": "MEMBERSHIP_PURCHASE", "totalAmount": 5600000, "transactionCount": 3},
                                          {"transactionType": "POST_FEE", "totalAmount": 1400000, "transactionCount": 2},
                                          {"transactionType": "PUSH_FEE", "totalAmount": 1400000, "transactionCount": 1}
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public ApiResponse<RevenueOverTimeResponse> getRevenueOverTime(
            @Parameter(description = "Start date (inclusive), defaults to 30 days ago", example = "2026-03-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-03-21")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate endDate = (to != null) ? to : LocalDate.now();
        LocalDate startDate = (from != null) ? from : endDate.minusDays(30);

        log.info("Admin requesting revenue data from {} to {}", startDate, endDate);

        RevenueOverTimeResponse response = adminDashboardService.getRevenueOverTime(startDate, endDate);

        return ApiResponse.<RevenueOverTimeResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/memberships/distribution")
    @Operation(
            summary = "Get active membership distribution by package level",
            description = "Returns the count and percentage of currently active memberships grouped by package level (BASIC, STANDARD, ADVANCED). Useful for a donut/pie chart.",
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
}
