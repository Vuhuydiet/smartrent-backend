package com.smartrent.service.ai;

/**
 * Queue of listings waiting for their AI analysis to be pre-computed.
 *
 * <p>A listing is enqueued the moment it is submitted or resubmitted, so its AI
 * analysis is ready within seconds instead of waiting for the next reconciliation
 * sweep. The queue is the primary trigger; the scheduler is only a backstop for
 * anything the queue missed (Redis down, message lost, listings that predate the
 * queue).
 */
public interface AiAnalysisQueue {

    /**
     * Enqueue a listing for AI pre-computation.
     *
     * <p>Best-effort: a queue failure must never fail the caller's transaction —
     * the reconciliation sweep will pick the listing up instead.
     */
    void enqueue(Long listingId);
}
