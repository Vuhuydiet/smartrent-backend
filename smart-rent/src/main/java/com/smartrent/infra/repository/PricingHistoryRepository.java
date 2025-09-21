package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Long> {

    // Get all pricing history for a listing, ordered by date
    List<PricingHistory> findByListingListingIdOrderByChangedAtDesc(Long listingId);

    // Get current price for a listing
    Optional<PricingHistory> findByListingListingIdAndIsCurrentTrue(Long listingId);

    // Get pricing history within date range
    List<PricingHistory> findByListingListingIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long listingId, LocalDateTime startDate, LocalDateTime endDate);

    // Get price changes by change type
    List<PricingHistory> findByListingListingIdAndChangeTypeOrderByChangedAtDesc(
            Long listingId, PricingHistory.PriceChangeType changeType);

    // Get listings with price changes in last N days
    @Query("SELECT DISTINCT ph.listing.listingId FROM pricing_histories ph " +
           "WHERE ph.changedAt >= :sinceDate AND ph.changeType != 'INITIAL'")
    List<Long> findDistinctListingIdsByChangedAtAfterAndChangeTypeNot(
            @Param("sinceDate") LocalDateTime sinceDate);

    // Get average price for a listing over time
    @Query("SELECT AVG(ph.newPrice) FROM pricing_histories ph WHERE ph.listing.listingId = :listingId")
    Optional<BigDecimal> findAverageNewPriceByListingListingId(@Param("listingId") Long listingId);

    // Get price statistics for a listing - keeping @Query as it's complex aggregation
    @Query("SELECT MIN(ph.newPrice) as minPrice, MAX(ph.newPrice) as maxPrice, AVG(ph.newPrice) as avgPrice " +
           "FROM pricing_histories ph WHERE ph.listing.listingId = :listingId")
    Object[] getPriceStatisticsByListingId(@Param("listingId") Long listingId);

    // Mark all previous prices as not current for a listing - requires @Modifying for update
    @Modifying
    @Query("UPDATE pricing_histories ph SET ph.isCurrent = false WHERE ph.listing.listingId = :listingId")
    void updateIsCurrentFalseByListingListingId(@Param("listingId") Long listingId);
}
