package com.smartrent.cronjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiListingVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiListingAutoModerationScheduler {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;
    private final AiListingVerificationService aiVerificationService;
    private final ObjectMapper objectMapper;

    /**
     * Run every hour to recover any listings stuck in IN_PROGRESS state.
     * This acts as a self-healing mechanism in case the server crashes during processing.
     */
    @Scheduled(fixedDelayString = "3600000") // 1 hour
    @org.springframework.transaction.annotation.Transactional
    public void recoverStuckInProgressListings() {
        log.info("Starting Self-Healing Job for stuck IN_PROGRESS listings...");
        try {
            java.time.LocalDateTime thresholdTime = java.time.LocalDateTime.now().minusMinutes(30);
            int recoveredCount = listingAiModerationRepository.resetStuckInProgressListings(thresholdTime);
            if (recoveredCount > 0) {
                log.info("Self-Healing Job successfully recovered {} listings stuck in IN_PROGRESS state.", recoveredCount);
            } else {
                log.debug("No stuck listings found.");
            }
        } catch (Exception e) {
            log.error("Error during Self-Healing Job for stuck IN_PROGRESS listings", e);
        }
    }

    /**
     * Run every 5 minutes to verify pending listings using AI.
     */
    @Scheduled(fixedDelayString = "${smartrent.ai.verification.scheduler.delay:300000}")
    public void processPendingListings() {
        log.info("Starting AI Auto Moderation Scheduler...");
        
        try {
            // Process in batches of 20
            Page<Listing> pendingListings = listingRepository.findListingsNeedingAiVerification(PageRequest.of(0, 20));
            List<Listing> listings = pendingListings.getContent();

            if (listings.isEmpty()) {
                log.info("No pending listings for AI verification.");
                return;
            }

            log.info("Found {} listings to process", listings.size());

            // 1. Get or create ListingAiModeration and mark as IN_PROGRESS
            List<ListingAiModeration> moderations = listings.stream().map(listing -> {
                ListingAiModeration moderation = listingAiModerationRepository.findById(listing.getListingId())
                    .orElse(ListingAiModeration.builder()
                        .listingId(listing.getListingId())
                        .retryCount(0)
                        .manualOverride(false)
                        .build());
                moderation.setVerificationStatus(VerificationStatus.IN_PROGRESS);
                return listingAiModerationRepository.saveAndFlush(moderation);
            }).toList();

            // 2. Process listings in parallel to speed up the batch process
            // Each listing will be processed in its own transaction (REQUIRES_NEW)
            java.util.stream.IntStream.range(0, listings.size()).parallel().forEach(i -> {
                try {
                    processSingleListing(listings.get(i), moderations.get(i));
                } catch (Exception e) {
                    log.error("Failed to process listing ID: {} in parallel batch", 
                        listings.get(i).getListingId(), e);
                }
            });

        } catch (Exception e) {
            log.error("Error in AI Auto Moderation Scheduler", e);
        }
    }

    @org.springframework.transaction.annotation.Transactional(
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW
    )
    public void processSingleListing(Listing listing, ListingAiModeration moderation) {
        try {
            log.info("Processing listing ID: {}", listing.getListingId());
            com.smartrent.dto.request.AiListingVerificationRequest request = 
                aiVerificationService.buildVerificationRequest(listing.getListingId());
            AiListingVerificationResponse response = aiVerificationService.verifyListing(request);
            
            moderation.setAiScore(response.getScore());
            try {
                // Save the ENTIRE response so admin can see all details (image/video issues, violations, etc.)
                moderation.setAiReason(objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize full AI response", e);
            }

            // Determine status based on AI suggested status
            String suggestedStatus = response.getSuggestedStatus();
            log.info("AI Suggested Status: {}", suggestedStatus);

            if ("APPROVED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.VERIFIED);
                listing.setVerified(true);
                listing.setModerationStatus(com.smartrent.enums.ModerationStatus.APPROVED);
            } else if ("REJECTED".equals(suggestedStatus)) {
                moderation.setVerificationStatus(VerificationStatus.REJECTED);
                listing.setVerified(false);
                listing.setModerationStatus(com.smartrent.enums.ModerationStatus.REJECTED);
            } else {
                // Default to UNDER_REVIEW for "NEEDS_REVIEW" or any other value
                moderation.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
                listing.setVerified(false);
                // Keep as PENDING_REVIEW or similar for manual review
                listing.setModerationStatus(com.smartrent.enums.ModerationStatus.PENDING_REVIEW);
            }

            listingAiModerationRepository.save(moderation);
            // Optionally, we might need to save Listing to update `verified` status
            listingRepository.save(listing);
            log.info("Finished processing listing ID: {}, Status: {}", listing.getListingId(), moderation.getVerificationStatus());

        } catch (Exception e) {
            log.error("Failed to verify listing ID: {}", listing.getListingId(), e);
            moderation.setRetryCount(moderation.getRetryCount() + 1);
            moderation.setVerificationStatus(VerificationStatus.PENDING); // Revert to pending for retry
            listingAiModerationRepository.save(moderation);
        }
    }
}
