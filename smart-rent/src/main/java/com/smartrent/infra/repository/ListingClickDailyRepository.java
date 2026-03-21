package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingClickDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ListingClickDailyRepository extends JpaRepository<ListingClickDaily, Long> {

    List<ListingClickDaily> findByListingIdOrderByClickDateDesc(Long listingId);

    List<ListingClickDaily> findByListingIdAndClickDateBetweenOrderByClickDateAsc(
            Long listingId, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query(value = "INSERT INTO listing_click_daily (listing_id, click_date, click_count) " +
            "VALUES (:listingId, :clickDate, :clickCount) " +
            "ON DUPLICATE KEY UPDATE click_count = :clickCount",
            nativeQuery = true)
    void upsertDailyCount(@Param("listingId") Long listingId,
                          @Param("clickDate") LocalDate clickDate,
                          @Param("clickCount") int clickCount);
}
