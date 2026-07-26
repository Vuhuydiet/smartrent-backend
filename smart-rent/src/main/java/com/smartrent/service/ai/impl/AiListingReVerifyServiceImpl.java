package com.smartrent.service.ai.impl;

import com.smartrent.dto.response.StoredAiModerationResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.service.ai.AiListingReVerifyService;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.service.ai.AiModerationProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default {@link AiListingReVerifyService}.
 *
 * <p>Lives in its own bean (rather than on {@link AiListingVerificationService})
 * to avoid a circular dependency: {@link AiModerationProcessorService} already
 * depends on {@link AiListingVerificationService}, so the orchestration that
 * wires the two together must sit outside both.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiListingReVerifyServiceImpl implements AiListingReVerifyService {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiModerationProcessorService aiModerationProcessorService;
    private final AiListingVerificationService aiListingVerificationService;

    @Override
    public StoredAiModerationResponse reVerifyAndStore(Long listingId) {
        log.info("Admin-triggered AI re-verification for listing ID: {}", listingId);

        // Fetch with amenities so the entity stays usable once detached:
        // processSingleListing runs in its own (REQUIRES_NEW) transaction. The
        // duplicate check no longer touches the address association — it reads
        // the denormalized location columns off the listing itself.
        Listing listing = listingRepository.findByIdWithAmenities(listingId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND));

        // Reuse the existing moderation row so history (retry count) and — crucially —
        // the human manual_override flag are preserved. A manually-verified post can be
        // re-analysed on demand without the AI ever clearing the admin's decision.
        ListingAiModeration moderation = listingAiModerationRepository.findById(listingId)
                .orElseGet(() -> ListingAiModeration.builder()
                        .listingId(listingId)
                        .retryCount(0)
                        .manualOverride(false)
                        .build());

        // Store-only: runs verify + duplicate check and writes the result onto the
        // moderation row (via the Spring proxy, so its REQUIRES_NEW transaction and
        // commit apply). Never approves/rejects and never touches manual_override.
        aiModerationProcessorService.processSingleListing(listing, moderation);

        StoredAiModerationResponse stored = aiListingVerificationService.getStoredModerationResult(listingId);
        if (stored == null) {
            // processSingleListing swallows AI failures (reverts the row to PENDING) — surface
            // that to the admin instead of returning an empty 200.
            throw new AppException(DomainCode.AI_SERVICE_ERROR);
        }
        return stored;
    }
}
