package com.smartrent.service.pricing;

import com.smartrent.dto.request.PriceUpdateRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.infra.repository.entity.Listing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PricingHistoryService {

    // Create initial pricing history when listing is created
    PricingHistoryResponse createInitialPricing(Long listingId, BigDecimal initialPrice, String priceUnit, String changedBy);

    // Update price and create pricing history
    PricingHistoryResponse updatePrice(Long listingId, PriceUpdateRequest request, String changedBy);

    // Record a price change made through a non-dedicated flow (e.g. the generic
    // listing edit form), where the caller already knows old/new price and has
    // already performed ownership checks. Backfills a baseline row first if the
    // listing has no pricing history yet, so the edit shows up as a real delta.
    void recordExternalPriceChange(Long listingId, BigDecimal oldPrice, Listing.PriceUnit oldPriceUnit,
                                    BigDecimal newPrice, Listing.PriceUnit newPriceUnit,
                                    String changedBy, String changeReason);

    // Get pricing history for a listing
    List<PricingHistoryResponse> getPricingHistoryByListingId(Long listingId);

    // Get pricing history for a listing with pagination
    PageResponse<PricingHistoryResponse> getPricingHistoryByListingId(Long listingId, int page, int size);

    // Get current price for a listing
    PricingHistoryResponse getCurrentPrice(Long listingId);

    // Get pricing history within date range
    List<PricingHistoryResponse> getPricingHistoryByDateRange(Long listingId, LocalDateTime startDate, LocalDateTime endDate);

    // Get pricing history within date range with pagination
    PageResponse<PricingHistoryResponse> getPricingHistoryByDateRange(Long listingId, LocalDateTime startDate, LocalDateTime endDate, int page, int size);

    // Get price statistics for a listing
    PriceStatistics getPriceStatistics(Long listingId);

    // Get listings with recent price changes
    List<Long> getListingsWithRecentPriceChanges(int daysBack);

    // Get listings with recent price changes with pagination
    PageResponse<Long> getListingsWithRecentPriceChanges(int daysBack, int page, int size);

    // Inner class for price statistics
    class PriceStatistics {
        public BigDecimal minPrice;
        public BigDecimal maxPrice;
        public BigDecimal avgPrice;
        public int totalChanges;
        public int priceIncreases;
        public int priceDecreases;

        public PriceStatistics(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal avgPrice,
                              int totalChanges, int priceIncreases, int priceDecreases) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.avgPrice = avgPrice;
            this.totalChanges = totalChanges;
            this.priceIncreases = priceIncreases;
            this.priceDecreases = priceDecreases;
        }
    }
}
