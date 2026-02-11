package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ListingDraft;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

