package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingAiModeration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ListingAiModerationRepository extends JpaRepository<ListingAiModeration, Long> {

    @Transactional
    @Modifying
    @Query("""
        UPDATE listing_ai_moderation lam
        SET lam.verificationStatus = 'IN_PROGRESS'
        WHERE lam.listingId IN :listingIds
        AND (lam.verificationStatus = 'PENDING' OR lam.verificationStatus IS NULL)
    """)
    int markListingsAsInProgress(@Param("listingIds") List<Long> listingIds);

    @Transactional
    @Modifying
    @Query("""
        UPDATE listing_ai_moderation lam
        SET lam.verificationStatus = 'PENDING'
        WHERE lam.verificationStatus = 'IN_PROGRESS'
        AND lam.updatedAt < :thresholdTime
    """)
    int resetStuckInProgressListings(@Param("thresholdTime") java.time.LocalDateTime thresholdTime);
}
