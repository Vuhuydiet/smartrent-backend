package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingModerationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingModerationEventRepository extends JpaRepository<ListingModerationEvent, Long> {

    /**
     * Get moderation timeline for a listing, most recent first.
     */
    List<ListingModerationEvent> findByListingIdOrderByCreatedAtDesc(Long listingId);
}
