package com.smartrent.controller;

import com.smartrent.dto.request.ListingReportRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.ReportReasonResponse;
import com.smartrent.service.report.ListingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Listing Reports", description = "APIs for reporting listings with incorrect or inappropriate information. Unauthenticated users can report listings.")
public class ListingReportController {

    ListingReportService listingReportService;

    @GetMapping("/reports/reasons")
    @Operation(
            summary = "Get all report reasons",
            description = "Retrieves all active report reasons that users can select when reporting a listing. This endpoint is public and does not require authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report reasons retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": [
                                                {
                                                  "reasonId": 1,
                                                  "reasonText": "Các thông tin về: giá, diện tích, mô tả",
                                                  "category": "LISTING",
                                                  "displayOrder": 1
                                                },
                                                {
                                                  "reasonId": 2,
                                                  "reasonText": "Ảnh",
                                                  "category": "LISTING",
                                                  "displayOrder": 2
                                                },
                                                {
                                                  "reasonId": 3,
                                                  "reasonText": "Trùng với tin rao khác",
                                                  "category": "LISTING",
                                                  "displayOrder": 3
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<ReportReasonResponse>> getReportReasons() {
        List<ReportReasonResponse> reasons = listingReportService.getReportReasons();
        return ApiResponse.<List<ReportReasonResponse>>builder()
                .data(reasons)
                .build();
    }

    @PostMapping("/{listingId}/reports")
    @Operation(
            summary = "Report a listing",
            description = "Submit a report about a listing with incorrect or inappropriate information. This endpoint is public and does not require authentication - anyone can report a listing."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report submitted successfully",
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
                                                "createdAt": "2024-01-20T10:30:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing required fields or invalid data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "code": "400001",
                                              "message": "At least one report reason must be selected",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found with ID: 123",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<ListingReportResponse> createReport(
            @Parameter(description = "Listing ID to report", required = true, example = "123")
            @PathVariable Long listingId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Report details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ListingReportRequest.class),
                            examples = @ExampleObject(
                                    name = "Report Request",
                                    value = """
                                            {
                                              "reasonIds": [1, 2, 3],
                                              "otherFeedback": "Giá không đúng với thực tế",
                                              "reporterName": "Nguyễn Văn A",
                                              "reporterPhone": "0912345678",
                                              "reporterEmail": "reporter@example.com",
                                              "category": "LISTING"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody ListingReportRequest request) {
        ListingReportResponse response = listingReportService.createReport(listingId, request);
        return ApiResponse.<ListingReportResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/reports")
    @Operation(
            summary = "Get report history for a listing",
            description = "Retrieves all reports submitted for a specific listing. This endpoint requires authentication and is typically used by admins or listing owners."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report history retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
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
                                                  "createdAt": "2024-01-20T10:30:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found with ID: 123",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<ListingReportResponse>> getReportHistory(
            @Parameter(description = "Listing ID", required = true, example = "123")
            @PathVariable Long listingId) {
        List<ListingReportResponse> reports = listingReportService.getReportHistory(listingId);
        return ApiResponse.<List<ListingReportResponse>>builder()
                .data(reports)
                .build();
    }
}

