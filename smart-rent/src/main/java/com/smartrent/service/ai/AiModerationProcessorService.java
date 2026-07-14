package com.smartrent.service.ai;

import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;

/**
 * Pre-computes AI analysis for listings in the background and stores it, so the
 * admin review dialog can display the result the moment it opens instead of
 * running the AI live and making the admin wait.
 *
 * <p>Store-only: it never approves or rejects a listing. The admin always makes
 * the final decision.
 *
 * <p>Separated from the scheduler so {@code @Transactional(REQUIRES_NEW)} is
 * properly applied via Spring's AOP proxy (self-invocation bypass fix).
 */
public interface AiModerationProcessorService {

    /**
     * Run the AI on a single listing and store the result, in its own transaction.
     *
     * @param listing    The listing to analyse.
     * @param moderation The associated AI moderation record (already IN_PROGRESS).
     */
    void processSingleListing(Listing listing, ListingAiModeration moderation);
}
