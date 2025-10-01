package com.smartrent.controller;

import com.smartrent.dto.request.PriceUpdateRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.service.pricing.PricingHistoryService;
import io.swagger.v3.oas.annotations.Operation;
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
            @RequestBody @Valid PriceUpdateRequest request,
            @RequestHeader("user-id") String userId) {
        PricingHistoryResponse response = pricingHistoryService.updatePrice(listingId, request, userId);
        return ApiResponse.<PricingHistoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/pricing-history")
    @Operation(
        summary = "Get full pricing history for a listing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Pricing history returned",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PricingHistoryResponse.class))
                )
            )
        }
    )
    ApiResponse<List<PricingHistoryResponse>> getPricingHistory(@PathVariable Long listingId) {
        List<PricingHistoryResponse> response = pricingHistoryService.getPricingHistoryByListingId(listingId);
        return ApiResponse.<List<PricingHistoryResponse>>builder()
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
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Pricing history for date range returned",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PricingHistoryResponse.class))
                )
            )
        }
    )
    ApiResponse<List<PricingHistoryResponse>> getPricingHistoryByDateRange(
            @PathVariable Long listingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<PricingHistoryResponse> response = pricingHistoryService.getPricingHistoryByDateRange(listingId, startDate, endDate);
        return ApiResponse.<List<PricingHistoryResponse>>builder()
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
        description = "Returns listing IDs that had price changes in the last N days (default 7).",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Listing IDs with recent price changes",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Long.class))
                )
            )
        }
    )
    ApiResponse<List<Long>> getListingsWithRecentPriceChanges(@RequestParam(defaultValue = "7") int daysBack) {
        List<Long> response = pricingHistoryService.getListingsWithRecentPriceChanges(daysBack);
        return ApiResponse.<List<Long>>builder()
                .data(response)
                .build();
    }
}
