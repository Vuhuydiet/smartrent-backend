package com.smartrent.event;

/**
 * Raised when a listing enters the review queue — on first submit, on resubmit
 * after a revision request, and on update-and-resubmit.
 *
 * <p>Consumed after the transaction commits to enqueue the listing for AI
 * pre-computation, so the admin review dialog has the analysis ready.
 *
 * @param listingId the listing that needs AI analysis
 */
public record ListingSubmittedEvent(Long listingId) {
}
