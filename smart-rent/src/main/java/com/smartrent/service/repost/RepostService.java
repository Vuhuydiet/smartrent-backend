package com.smartrent.service.repost;

import com.smartrent.dto.request.RepostListingRequest;
import com.smartrent.dto.response.RepostResponse;

/**
 * Service for re-publishing expired listings ("đăng lại").
 *
 * <p>Mirrors {@link com.smartrent.service.push.PushService} — the user picks
 * between consuming the matching membership quota for their vipType (e.g.
 * POST_GOLD for a GOLD listing) or paying the per-day listing fee via VNPay.
 *
 * <p>Successful repost flips {@code expired = false}, sets {@code expiryDate
 * = now + durationDays}, refreshes {@code postDate} and {@code pushedAt} so
 * the listing returns to the top of search results.
 */
public interface RepostService {

    /**
     * Repost an expired listing.
     *
     * @param userId  Authenticated user (must be the listing owner).
     * @param request Repost preferences (quota vs payment, duration).
     * @return Result describing what happened — either the new active period
     *         (quota path) or a paymentUrl to redirect the user to (payment
     *         path).
     */
    RepostResponse repostListing(String userId, RepostListingRequest request);

    /**
     * Finalize a repost after a direct-payment transaction has completed.
     * Called from the payment provider callback.
     */
    RepostResponse completeRepostAfterPayment(String transactionId);
}
