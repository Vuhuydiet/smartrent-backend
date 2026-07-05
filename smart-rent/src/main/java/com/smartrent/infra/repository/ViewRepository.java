package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.View;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    long countByListing_ListingId(Long listingId);

    long countByListing_ListingIdAndViewedAtBetween(Long listingId, LocalDateTime start, LocalDateTime end);

    boolean existsByIpAddressAndListing_ListingIdAndViewedAtAfter(
            String ipAddress, Long listingId, LocalDateTime after);
}
