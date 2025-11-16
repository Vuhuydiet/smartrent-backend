package com.smartrent.controller;

import com.smartrent.dto.request.PriceUpdateRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.service.pricing.PricingHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/listings")
@Tag(name = "Pricing", description = "Pricing & Price History endpoints for listings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PricingHistoryController {

    PricingHistoryService pricingHistoryService;

    @PutMapping("/{listingId}/price")
    @Operation(
        summary = "Update current price for a listing",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PriceUpdateRequest.class),
                examples = @ExampleObject(
                    name = "Update Price Example",
                    value = """
                        { "newPrice": 1350.00, "effectiveAt": "2025-09-21T10:00:00" }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Price updated and history recorded",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PricingHistoryResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    ApiResponse<PricingHistoryResponse> updatePrice(
            @PathVariable Long listingId,
            @RequestBody @Valid PriceUpdateRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        PricingHistoryResponse response = pricingHistoryService.updatePrice(listingId, request, userId);
        return ApiResponse.<PricingHistoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/pricing-history")
    @Operation(
        summary = "Get full pricing history for a listing",
        description = "Returns pricing history for a listing with pagination",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Pricing history returned",
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
                                "size": 10,
                                "totalElements": 15,
                                "totalPages": 2,
                                "data": [
                                  {
                                    "priceHistoryId": 1,
                                    "listingId": 123,
                                    "oldPrice": 1000.00,
                                    "newPrice": 1200.00,
                                    "changeType": "INCREASE",
                                    "changePercentage": 20.00,
                                    "changedAt": "2024-01-01T00:00:00"
                                  }
                                ]
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    ApiResponse<PageResponse<PricingHistoryResponse>> getPricingHistory(
            @PathVariable Long listingId,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PricingHistoryResponse> response = pricingHistoryService.getPricingHistoryByListingId(listingId, page, size);
        return ApiResponse.<PageResponse<PricingHistoryResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/current-price")
    @Operation(
        summary = "Get current price for a listing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Current price returned",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PricingHistoryResponse.class)
                )
            )
        }
    )
    ApiResponse<PricingHistoryResponse> getCurrentPrice(@PathVariable Long listingId) {
        PricingHistoryResponse response = pricingHistoryService.getCurrentPrice(listingId);
        return ApiResponse.<PricingHistoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/pricing-history/date-range")
    @Operation(
        summary = "Get pricing history within a date range",
        description = "Returns pricing history for a listing within a date range with pagination",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Pricing history for date range returned",
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
                                "size": 10,
                                "totalElements": 8,
                                "totalPages": 1,
                                "data": [
                                  {
                                    "priceHistoryId": 1,
                                    "listingId": 123,
                                    "oldPrice": 1000.00,
                                    "newPrice": 1200.00,
                                    "changeType": "INCREASE",
                                    "changePercentage": 20.00,
                                    "changedAt": "2024-01-01T00:00:00"
                                  }
                                ]
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    ApiResponse<PageResponse<PricingHistoryResponse>> getPricingHistoryByDateRange(
            @PathVariable Long listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PricingHistoryResponse> response = pricingHistoryService.getPricingHistoryByDateRange(listingId, startDate, endDate, page, size);
        return ApiResponse.<PageResponse<PricingHistoryResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/price-statistics")
    @Operation(
        summary = "Get price statistics for a listing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Price statistics returned",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PricingHistoryService.PriceStatistics.class)
                )
            )
        }
    )
    ApiResponse<PricingHistoryService.PriceStatistics> getPriceStatistics(@PathVariable Long listingId) {
        PricingHistoryService.PriceStatistics response = pricingHistoryService.getPriceStatistics(listingId);
        return ApiResponse.<PricingHistoryService.PriceStatistics>builder()
                .data(response)
                .build();
    }

    @GetMapping("/recent-price-changes")
    @Operation(
        summary = "List listing IDs with recent price changes",
        description = "Returns listing IDs that had price changes in the last N days (default 7) with pagination.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing IDs with recent price changes",
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
                                "size": 10,
                                "totalElements": 25,
                                "totalPages": 3,
                                "data": [123, 456, 789, 101, 112]
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    ApiResponse<PageResponse<Long>> getListingsWithRecentPriceChanges(
            @Parameter(description = "Number of days to look back", example = "7")
            @RequestParam(defaultValue = "7") int daysBack,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<Long> response = pricingHistoryService.getListingsWithRecentPriceChanges(daysBack, page, size);
        return ApiResponse.<PageResponse<Long>>builder()
                .data(response)
                .build();
    }
}
