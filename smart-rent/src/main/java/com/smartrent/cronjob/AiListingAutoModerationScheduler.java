package com.smartrent.cronjob;

import com.smartrent.infra.repository.ListingAiModerationRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.ListingAiModeration;
import com.smartrent.service.ai.AiModerationExecutor;
import com.smartrent.service.ai.AiModerationProcessorService;
import com.smartrent.service.ai.AiVerificationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * <b>Reconciliation backstop</b> for AI pre-computation — not the primary trigger.
 *
 * <p>A listing is normally analysed within seconds of being submitted, pushed onto
 * the Redis Streams queue by {@code ListingSubmittedAiEnqueuer}. This sweep exists
 * for the listings that never made it onto that queue and would otherwise be
 * analysed by nobody, leaving the admin staring at an empty dialog forever:
 * <ul>
 *   <li>Redis was down (or the enqueue threw) at submit time.</li>
 *   <li>The consumer died mid-message and the entry aged out of the pending list.</li>
 *   <li>The listing predates the queue, or was submitted while auto-verify was OFF
 *       and the admin has since switched it back ON.</li>
 * </ul>
 *
 * <p>Because the queue does the real work, this runs infrequently (30 min) on a
 * small batch — it is a safety net, not a throughput mechanism.
 *
 * <p><b>Store-only.</b> This never approves or rejects a listing — it only stores
 * the AI's analysis on {@link ListingAiModeration}. Every listing still lands in
 * the admin's review queue and the admin makes the final call in the dialog.
 *
 * <p>Two independent switches gate it:
 * <ul>
 *   <li>{@code smartrent.ai.verification.scheduler.enabled} — infra-level kill switch
 *       (env/config, e.g. to keep it off in tests or local dev).</li>
 *   <li>The {@code ai_auto_verify_enabled} setting in the DB — the admin-facing
 *       toggle, read fresh on every tick so it takes effect immediately and
 *       survives restarts. See {@link AiVerificationSettingService}.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
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

    private final AiModerationExecutor aiModerationExecutor;

    /** DB-backed admin toggle — read on every tick, so no restart is needed. */
    private final AiVerificationSettingService aiVerificationSettingService;

    /**
     * Number of listings pulled per sweep. Deliberately small: this only picks up
     * stragglers the queue missed, and a large batch would pin the single AI
     * worker's CPU with concurrent duplicate checks for no throughput gain (the
     * GIL serializes them anyway). Raise it temporarily to drain a big backlog.
     */
    @Value("${smartrent.ai.verification.scheduler.batch-size:10}")
    private int batchSize;

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
     * Sweep every 30 minutes for listings the queue never analysed, and store their
     * AI result. Finds nothing on a healthy system — the queue got there first.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Load a page of pending listings in a single read transaction.</li>
     *   <li>Batch-mark all as IN_PROGRESS in one query before dispatching — this also
     *       stops the queue consumer double-analysing anything picked up here.</li>
     *   <li>Call {@link AiModerationProcessorService#processSingleListing} for each listing
     *       via the Spring proxy so each item runs in its own {@code REQUIRES_NEW} transaction.
     *       Tasks run concurrently on {@link AiModerationExecutor#batchPool()} — a dedicated
     *       I/O pool sized for the batch — instead of the shared ForkJoinPool which is
     *       designed for CPU-bound work and limited to {@code cores - 1} threads.</li>
     * </ol>
     */
    @Scheduled(fixedDelayString = "${smartrent.ai.verification.scheduler.delay:1800000}")
    public void processPendingListings() {
        if (!aiVerificationSettingService.isAutoVerifyEnabled()) {
            log.debug("AI auto-verify is DISABLED by admin — skipping reconciliation sweep.");
            return;
        }
        log.debug("Starting AI reconciliation sweep...");

        try {
            int size = batchSize > 0 ? batchSize : 10;
            Page<Listing> pendingListings = listingRepository.findListingsNeedingAiVerification(PageRequest.of(0, size));
            List<Listing> listings = pendingListings.getContent();

            if (listings.isEmpty()) {
                log.debug("Reconciliation sweep found nothing — the queue kept up.");
                return;
            }

            // Not empty means the queue missed these — worth surfacing at INFO.
            log.info("Reconciliation sweep picked up {} listing(s) the queue missed", listings.size());

            // 1. Upsert moderation records, then batch-mark all as IN_PROGRESS in one query.
            List<ListingAiModeration> moderations = listings.stream().map(listing ->
                listingAiModerationRepository.findById(listing.getListingId())
                    .orElseGet(() -> ListingAiModeration.builder()
                        .listingId(listing.getListingId())
                        .retryCount(0)
                        .manualOverride(false)
                        .build())
            ).toList();
            listingAiModerationRepository.saveAll(moderations);

            List<Long> listingIds = listings.stream().map(Listing::getListingId).toList();
            listingAiModerationRepository.markListingsAsInProgress(listingIds);

            // 2. Process listings concurrently on the dedicated I/O thread pool.
            //    Each call goes through the Spring AOP proxy on aiModerationProcessorService,
            //    so @Transactional(REQUIRES_NEW) is correctly applied per listing.
            List<CompletableFuture<Void>> futures = IntStream.range(0, listings.size())
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        aiModerationProcessorService.processSingleListing(listings.get(i), moderations.get(i));
                    } catch (Exception e) {
                        log.error("Failed to pre-compute listing ID: {} in parallel batch",
                            listings.get(i).getListingId(), e);
                    }
                }, aiModerationExecutor.batchPool()))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("Error in AI background pre-computation", e);
        }
    }
}
