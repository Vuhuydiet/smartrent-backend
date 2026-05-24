package com.smartrent.service.ai;

import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;

/**
 * Service for processing individual listings through AI moderation.
 * Separated from the scheduler to ensure @Transactional(REQUIRES_NEW)
 * is properly applied via Spring's AOP proxy (self-invocation bypass fix).
 */
public interface AiModerationProcessorService {

    /**
     * Process a single listing through AI moderation in its own transaction.
     *
     * @param listing    The listing entity to moderate.
     * @param moderation The associated AI moderation record (already IN_PROGRESS).
     */
    void processSingleListing(Listing listing, ListingAiModeration moderation);
}
