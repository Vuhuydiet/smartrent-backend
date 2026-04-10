package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingDraftRepository extends JpaRepository<ListingDraft, Long> {

    /**
     * Find all drafts for a user, ordered by updated time descending
     */
    List<ListingDraft> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Find a draft by ID and user ID (for ownership verification)
     */
    Optional<ListingDraft> findByDraftIdAndUserId(Long draftId, String userId);

    /**
     * Count drafts for a user
     */
    long countByUserId(String userId);

    /**
     * Delete all drafts for a user
     */
    void deleteByUserId(String userId);

    /**
     * Return every non-empty media_ids string currently referenced by any draft. Used by the
     * orphan media cleanup cron to avoid deleting media that is still attached to a long-lived
     * draft. The strings are comma-separated media IDs; the caller parses them into a Set.
     */
    @Query("SELECT d.mediaIds FROM ListingDraft d WHERE d.mediaIds IS NOT NULL AND d.mediaIds <> ''")
    List<String> findAllDraftMediaIdsStrings();
}

