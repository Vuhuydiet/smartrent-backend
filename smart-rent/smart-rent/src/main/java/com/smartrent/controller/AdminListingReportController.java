package com.smartrent.controller;

import com.smartrent.dto.request.ResolveReportRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.service.report.ListingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - Listing Reports", description = "Admin APIs for managing and resolving listing reports")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminListingReportController {

    ListingReportService listingReportService;

    @GetMapping
    @Operation(
            summary = "Get all listing reports (Admin)",
            description = "Retrieves all listing reports with pagination and optional status filter. Only accessible by admins."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reports retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "page": 1,
                                                "size": 20,
                                                "totalElements": 45,
                                                "totalPages": 3,
                                                "data": [
                                                  {
                                                    "reportId": 1,
                                                    "listingId": 123,
                                                    "reporterName": "Nguyễn Văn A",
                                                    "reporterPhone": "0912345678",
                                                    "reporterEmail": "reporter@example.com",
                                                    "reportReasons": [
                                                      {
                                                        "reasonId": 1,
                                                        "reasonText": "Các thông tin về: giá, diện tích, mô tả",
                                                        "category": "LISTING",
                                                        "displayOrder": 1
                                                      }
                                                    ],
                                                    "otherFeedback": "Giá không đúng với thực tế",
                                                    "category": "LISTING",
                                                    "status": "PENDING",
                                                    "resolvedBy": null,
                                                    "resolvedByName": null,
                                                    "resolvedAt": null,
                                                    "adminNotes": null,
                                                    "createdAt": "2024-01-20T10:30:00",
                                                    "updatedAt": "2024-01-20T10:30:00"
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Admin authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<ListingReportResponse>> getAllReports(
            @Parameter(description = "Filter by status (PENDING, RESOLVED, REJECTED)", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        PageResponse<ListingReportResponse> reports = listingReportService.getAllReports(status, page, size);
        return ApiResponse.<PageResponse<ListingReportResponse>>builder()
                .data(reports)
                .build();
    }

    @GetMapping("/{reportId}")
    @Operation(
            summary = "Get report by ID (Admin)",
            description = "Retrieves detailed information about a specific report. Only accessible by admins."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "reportId": 1,
                                                "listingId": 123,
                                                "reporterName": "Nguyễn Văn A",
                                                "reporterPhone": "0912345678",
                                                "reporterEmail": "reporter@example.com",
                                                "reportReasons": [
                                                  {
                                                    "reasonId": 1,
                                                    "reasonText": "Các thông tin về: giá, diện tích, mô tả",
                                                    "category": "LISTING",
                                                    "displayOrder": 1
                                                  }
                                                ],
                                                "otherFeedback": "Giá không đúng với thực tế",
                                                "category": "LISTING",
                                                "status": "RESOLVED",
                                                "resolvedBy": "admin-123",
                                                "resolvedByName": "John Doe",
                                                "resolvedAt": "2024-01-21T14:30:00",
                                                "adminNotes": "Verified and contacted listing owner",
                                                "createdAt": "2024-01-20T10:30:00",
                                                "updatedAt": "2024-01-21T14:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Report not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Report not found with ID: 1",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<ListingReportResponse> getReportById(
            @Parameter(description = "Report ID", required = true, example = "1")
            @PathVariable Long reportId) {
        
        ListingReportResponse report = listingReportService.getReportById(reportId);
        return ApiResponse.<ListingReportResponse>builder()
                .data(report)
                .build();
    }

    @PutMapping("/{reportId}/resolve")
    @Operation(
            summary = "Resolve or reject a report (Admin)",
            description = "Admin can resolve or reject a pending report with optional notes. Only accessible by admins."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report resolved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "reportId": 1,
                                                "listingId": 123,
                                                "reporterName": "Nguyễn Văn A",
                                                "reporterPhone": "0912345678",
                                                "reporterEmail": "reporter@example.com",
                                                "reportReasons": [
                                                  {
                                                    "reasonId": 1,
                                                    "reasonText": "Các thông tin về: giá, diện tích, mô tả",
                                                    "category": "LISTING",
                                                    "displayOrder": 1
                                                  }
                                                ],
                                                "otherFeedback": "Giá không đúng với thực tế",
                                                "category": "LISTING",
                                                "status": "RESOLVED",
                                                "resolvedBy": "admin-123",
                                                "resolvedByName": "John Doe",
                                                "resolvedAt": "2024-01-21T14:30:00",
                                                "adminNotes": "Verified and contacted listing owner",
                                                "createdAt": "2024-01-20T10:30:00",
                                                "updatedAt": "2024-01-21T14:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - report already resolved or invalid status",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Already Resolved",
                                    value = """
                                            {
                                              "code": "400001",
                                              "message": "Report has already been resolved",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Report not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Report not found with ID: 1",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<ListingReportResponse> resolveReport(
            @Parameter(description = "Report ID", required = true, example = "1")
            @PathVariable Long reportId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Resolution details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResolveReportRequest.class),
                            examples = @ExampleObject(
                                    name = "Resolve Request",
                                    value = """
                                            {
                                              "status": "RESOLVED",
                                              "adminNotes": "Verified the issue and contacted the listing owner to update the information"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody ResolveReportRequest request) {
        
        // Extract admin ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();
        
        ListingReportResponse report = listingReportService.resolveReport(reportId, request, adminId);
        return ApiResponse.<ListingReportResponse>builder()
                .data(report)
                .build();
    }

    @GetMapping("/statistics")
    @Operation(
            summary = "Get report statistics (Admin)",
            description = "Retrieves statistics about all reports including counts by status. Only accessible by admins."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "totalReports": 150,
                                                "pendingReports": 45,
                                                "resolvedReports": 85,
                                                "rejectedReports": 20
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<Map<String, Long>> getReportStatistics() {
        ListingReportService.ReportStatistics stats = listingReportService.getReportStatistics();
        
        Map<String, Long> response = new HashMap<>();
        response.put("totalReports", stats.getTotalReports());
        response.put("pendingReports", stats.getPendingReports());
        response.put("resolvedReports", stats.getResolvedReports());
        response.put("rejectedReports", stats.getRejectedReports());
        
        return ApiResponse.<Map<String, Long>>builder()
                .data(response)
                .build();
    }
}

