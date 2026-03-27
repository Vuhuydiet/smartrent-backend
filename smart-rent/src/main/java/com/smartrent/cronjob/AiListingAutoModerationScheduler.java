package com.smartrent.cronjob;

import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.ai.AiListingVerificationService;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Periodic AI-based auto moderation for listings.
 *
 * This scheduler finds listings that have not yet been auto-verified and
 * sends them to the AI verification service. Based on the AI score and
 * confidence, it updates listing verification flags so that good listings
 * can be auto-approved while others stay pending or effectively rejected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiListingAutoModerationScheduler {

    ListingRepository listingRepository;
    AiListingVerificationService aiListingVerificationService;

    /**
     * Run AI auto-moderation every 15 minutes.
     * Cron: second, minute, hour, day, month, day-of-week.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void runAiAutoModeration() {
        log.info("=== Starting AI auto-moderation for listings ===");

        List<Listing> candidates = listingRepository.findListingsNeedingAiVerification();
        if (candidates.isEmpty()) {
            log.info("No listings needing AI verification at this time");
            return;
        }

        log.info("Found {} listings needing AI verification", candidates.size());

        for (Listing listing : candidates) {
            try {
                processListingWithAi(listing);
            } catch (AppException ex) {
                // If AI service is temporarily unavailable, stop processing to avoid hammering the service
                if (ex.getErrorCode() == DomainCode.AI_SERVICE_UNAVAILABLE
                        || ex.getErrorCode() == DomainCode.AI_SERVICE_TIMEOUT) {
                    log.warn("AI service unavailable/timeout while processing listing {}: {}. Stopping batch.",
                            listing.getListingId(), ex.getMessage());
                    break;
                }
                log.error("AI error for listing {}: {}", listing.getListingId(), ex.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during AI moderation for listing {}: {}", listing.getListingId(), e.getMessage(), e);
            }
        }

        log.info("=== Completed AI auto-moderation for listings ===");
    }

    private void processListingWithAi(Listing listing) {
        log.info("Running AI verification for listing {} - title='{}'", listing.getListingId(), listing.getTitle());

        AiListingVerificationResponse ai = aiListingVerificationService.verifyListing(listing);

        if (ai == null) {
            log.warn("AI verification returned null for listing {}", listing.getListingId());
            return;
        }

        Double score = ai.getScore();
        Double confidence = ai.getConfidence();

        // Default to moderate risk if values are missing
        double s = score != null ? score : 0.5;
        double c = confidence != null ? confidence : 0.5;

        boolean hasHighRiskViolation = ai.getViolations() != null && ai.getViolations().stream()
                .anyMatch(v -> "high".equalsIgnoreCase(v.getSeverity()) || "critical".equalsIgnoreCase(v.getSeverity()));

        // Case 1: Good listing - auto approve (soft)
        if (s >= 0.8 && c >= 0.8 && !hasHighRiskViolation && Boolean.FALSE.equals(listing.getIsDraft())) {
            listing.setVerified(true);      // counts as approved/visible
            listing.setIsVerify(true);      // mark that AI has processed this listing
            log.info("Listing {} auto-approved by AI (score={}, confidence={})", listing.getListingId(), s, c);
        }
        // Case 2: Medium risk - keep pending review (IN_REVIEW)
        else if (s >= 0.5 && !hasHighRiskViolation) {
            listing.setVerified(false);
            listing.setIsVerify(true);      // marked as under review
            log.info("Listing {} kept in review by AI (score={}, confidence={})", listing.getListingId(), s, c);
        }
        // Case 3: High risk - effectively rejected by AI
        else {
            listing.setVerified(false);
            listing.setIsVerify(false);     // legacy rejected state when postDate exists
            log.info("Listing {} flagged as high risk by AI (score={}, confidence={})", listing.getListingId(), s, c);
        }

        listingRepository.save(listing);
    }
}
