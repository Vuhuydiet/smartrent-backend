package com.smartrent.cronjob;

import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.infra.repository.entity.enums.VerificationStatus;
import com.smartrent.service.ai.AiModerationProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "smartrent.ai.verification.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AiListingAutoModerationScheduler {

    private final ListingRepository listingRepository;
    private final ListingAiModerationRepository listingAiModerationRepository;

    /**
     * Injected as a Spring proxy bean so that @Transactional(REQUIRES_NEW)
     * in processSingleListing is correctly intercepted by AOP.
     */
    private final AiModerationProcessorService aiModerationProcessorService;

    /**
     * Runtime toggle — controlled via the admin API.
     * Defaults to {@code true} so the scheduler runs on startup.
     * Can be toggled at runtime without a server restart.
     */
    private final AtomicBoolean aiAutoModerationEnabled = new AtomicBoolean(true);

    /** Called by the admin API to enable AI auto-moderation. */
    public void enable() {
        aiAutoModerationEnabled.set(true);
        log.info("AI Auto Moderation Scheduler has been ENABLED by admin.");
    }

    /** Called by the admin API to disable AI auto-moderation. */
    public void disable() {
        aiAutoModerationEnabled.set(false);
        log.info("AI Auto Moderation Scheduler has been DISABLED by admin.");
    }

    /** Returns the current toggle state. */
    public boolean isEnabled() {
        return aiAutoModerationEnabled.get();
    }

    /**
     * Run every hour to recover any listings stuck in IN_PROGRESS state.
     * This acts as a self-healing mechanism in case the server crashes during processing.
     */
    @Scheduled(fixedDelayString = "3600000") // 1 hour
    @Transactional
    public void recoverStuckInProgressListings() {
        log.info("Starting Self-Healing Job for stuck IN_PROGRESS listings...");
        try {
            LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(30);
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
     *
     * <p>Strategy:
     * <ol>
     *   <li>Load a page of pending listings in a single read transaction.</li>
     *   <li>Mark each as IN_PROGRESS (batch commit) before dispatching to AI.</li>
     *   <li>Call {@link AiModerationProcessorService#processSingleListing} for each listing
     *       via the Spring proxy so each item runs in its own {@code REQUIRES_NEW} transaction.</li>
     * </ol>
     */
    @Scheduled(fixedDelayString = "${smartrent.ai.verification.scheduler.delay:300000}")
    public void processPendingListings() {
        if (!aiAutoModerationEnabled.get()) {
            log.debug("AI Auto Moderation Scheduler is currently DISABLED — skipping batch.");
            return;
        }
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

            // 1. Mark each listing as IN_PROGRESS before dispatching
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

            // 2. Process listings in parallel.
            //    Each call goes through the Spring AOP proxy on aiModerationProcessorService,
            //    so @Transactional(REQUIRES_NEW) is correctly applied per listing.
            IntStream.range(0, listings.size()).parallel().forEach(i -> {
                try {
                    aiModerationProcessorService.processSingleListing(listings.get(i), moderations.get(i));
                } catch (Exception e) {
                    log.error("Failed to process listing ID: {} in parallel batch",
                        listings.get(i).getListingId(), e);
                }
            });

        } catch (Exception e) {
            log.error("Error in AI Auto Moderation Scheduler", e);
        }
    }
}
