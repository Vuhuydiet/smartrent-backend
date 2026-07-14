package com.smartrent.service.ai.impl;

import com.smartrent.event.ListingSubmittedEvent;
import com.smartrent.service.ai.AiAnalysisQueue;
import com.smartrent.service.ai.AiVerificationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Enqueues a submitted listing for AI pre-computation — but only while the admin's
 * auto-verify setting is ON. With it OFF, nothing is pushed onto the queue at all,
 * so no AI spend is incurred for listings submitted during that window; admins run
 * the analysis by hand from the review dialog instead.
 *
 * <p>Fires on {@link TransactionPhase#AFTER_COMMIT} on purpose: enqueueing inside
 * the transaction would hand the worker a listing ID that a later rollback makes
 * nonexistent, and the worker (running in a different thread) could even race the
 * commit and read a listing that isn't there yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ListingSubmittedAiEnqueuer {

    private final AiAnalysisQueue aiAnalysisQueue;
    private final AiVerificationSettingService aiVerificationSettingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onListingSubmitted(ListingSubmittedEvent event) {
        if (!aiVerificationSettingService.isAutoVerifyEnabled()) {
            log.debug("AI auto-verify is OFF — not queueing listing {}", event.listingId());
            return;
        }
        log.debug("Listing {} submitted — queueing AI analysis", event.listingId());
        aiAnalysisQueue.enqueue(event.listingId());
    }
}
