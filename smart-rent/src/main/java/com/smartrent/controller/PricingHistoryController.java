package com.smartrent.controller;

import com.smartrent.controller.dto.request.PriceUpdateRequest;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.PricingHistoryResponse;
import com.smartrent.service.pricing.PricingHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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
    ApiResponse<List<PricingHistoryResponse>> getPricingHistory(@PathVariable Long listingId) {
        List<PricingHistoryResponse> response = pricingHistoryService.getPricingHistoryByListingId(listingId);
        return ApiResponse.<List<PricingHistoryResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/current-price")
    ApiResponse<PricingHistoryResponse> getCurrentPrice(@PathVariable Long listingId) {
        PricingHistoryResponse response = pricingHistoryService.getCurrentPrice(listingId);
        return ApiResponse.<PricingHistoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/{listingId}/pricing-history/date-range")
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
    ApiResponse<PricingHistoryService.PriceStatistics> getPriceStatistics(@PathVariable Long listingId) {
        PricingHistoryService.PriceStatistics response = pricingHistoryService.getPriceStatistics(listingId);
        return ApiResponse.<PricingHistoryService.PriceStatistics>builder()
                .data(response)
                .build();
    }

    @GetMapping("/recent-price-changes")
    ApiResponse<List<Long>> getListingsWithRecentPriceChanges(@RequestParam(defaultValue = "7") int daysBack) {
        List<Long> response = pricingHistoryService.getListingsWithRecentPriceChanges(daysBack);
        return ApiResponse.<List<Long>>builder()
                .data(response)
                .build();
    }
}
