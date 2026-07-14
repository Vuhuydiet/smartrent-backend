package com.smartrent.service.ai.impl;

import com.smartrent.enums.ModerationStatus;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiModerationProcessorService;
import com.smartrent.service.ai.AiVerificationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Analyses one listing pulled off the AI analysis queue: claims it, then hands it
 * to the store-only {@link AiModerationProcessorService}.
 *
 * <p>Applies the same gates as the reconciliation sweep's query
 * ({@code findListingsNeedingAiVerification}) — the queue can deliver a listing
 * that has since been drafted, approved by an admin, or taken over manually, and
 * re-analysing it would be wasted AI spend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisWorker {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiModerationProcessorService aiModerationProcessorService;
    private final AiVerificationSettingService aiVerificationSettingService;

    /**
     * Pre-compute and store the AI analysis for one listing.
     *
     * <p>Silently no-ops when the listing no longer needs analysis. Never approves
     * or rejects — see {@link AiModerationProcessorService}.
     */
    public void analyze(Long listingId) {
        if (!aiVerificationSettingService.isAutoVerifyEnabled()) {
            log.debug("AI auto-verify disabled — dropping queued analysis for listing {}", listingId);
            return;
        }

        // Fetch-joins address + amenities: processSingleListing reads them off this
        // (detached) entity when building the duplicate-check request.
        Listing listing = listingRepository.findByIdWithAmenities(listingId).orElse(null);
        if (listing == null) {
            log.warn("Queued listing {} no longer exists — skipping AI analysis", listingId);
            return;
        }
        if (!needsAnalysis(listing)) {
            log.debug("Listing {} no longer needs AI analysis (draft/shadow/resolved) — skipping", listingId);
            return;
        }

        ListingAiModeration moderation = listingAiModerationRepository.findById(listingId)
                .orElseGet(() -> ListingAiModeration.builder()
                        .listingId(listingId)
                        .retryCount(0)
                        .manualOverride(false)
                        .verificationStatus(VerificationStatus.PENDING)
                        .build());

        if (Boolean.TRUE.equals(moderation.getManualOverride())) {
            log.debug("Listing {} was taken over by an admin — skipping AI analysis", listingId);
            return;
        }
        if (moderation.getRetryCount() != null && moderation.getRetryCount() >= 3) {
            log.debug("Listing {} exhausted AI retries — skipping", listingId);
            return;
        }
        VerificationStatus status = moderation.getVerificationStatus();
        if (status == VerificationStatus.IN_PROGRESS) {
            log.debug("Listing {} is already being analysed — skipping duplicate delivery", listingId);
            return;
        }
        // Anything already computed (VERIFIED/REJECTED/UNDER_REVIEW) is only re-run
        // via a resubmit, which resets the record back to PENDING first.
        if (status != null && status != VerificationStatus.PENDING) {
            log.debug("Listing {} already has a stored AI result ({}) — skipping", listingId, status);
            return;
        }

        // Claim it so a concurrent sweep doesn't pick up the same listing.
        moderation.setVerificationStatus(VerificationStatus.IN_PROGRESS);
        listingAiModerationRepository.save(moderation);

        aiModerationProcessorService.processSingleListing(listing, moderation);
    }

    /** Mirrors the gatekeeping half of {@code findListingsNeedingAiVerification}. */
    private boolean needsAnalysis(Listing listing) {
        if (Boolean.TRUE.equals(listing.getIsDraft())) return false;
        if (Boolean.TRUE.equals(listing.getIsShadow())) return false;
        if (listing.getPostDate() == null) return false;
        if (Boolean.TRUE.equals(listing.getVerified())) return false;

        ModerationStatus ms = listing.getModerationStatus();
        return ms == null
                || ms == ModerationStatus.PENDING_REVIEW
                || ms == ModerationStatus.RESUBMITTED;
    }
}
