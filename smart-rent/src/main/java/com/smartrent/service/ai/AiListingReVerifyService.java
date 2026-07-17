package com.smartrent.service.ai;

import com.smartrent.dto.response.StoredAiModerationResponse;

/**
 * Admin-triggered, on-demand AI re-verification for an existing listing.
 *
 * <p>Unlike {@link AiListingVerificationService#verifyListing} (stateless, returns
 * a one-off analysis) this <b>persists</b> the fresh result onto
 * {@code listing_ai_moderation}, so the very next
 * {@link AiListingVerificationService#getStoredModerationResult} — i.e. what the
 * admin review dialog loads when the post is opened — reflects this latest run.
 *
 * <p>It reuses the exact same store-only pipeline the background auto-moderation
 * job uses ({@link AiModerationProcessorService#processSingleListing}), so the
 * stored shape (verification + duplicateCheck) is identical. It never approves or
 * rejects the listing and never touches the human {@code manual_override} flag —
 * a manually-verified post can be re-analysed without losing the admin's decision.
 */
public interface AiListingReVerifyService {

    /**
     * Re-run AI moderation (verify + duplicate check) for a listing by ID, persist
     * the result as the latest stored moderation, and return it.
     *
     * @param listingId The listing to re-verify.
     * @return The freshly stored moderation result (verification + duplicate check).
     */
    StoredAiModerationResponse reVerifyAndStore(Long listingId);
}
