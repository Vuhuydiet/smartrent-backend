package com.smartrent.service.push;

import com.smartrent.dto.request.PushListingRequest;
import com.smartrent.dto.request.SchedulePushRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PushResponse;
import com.smartrent.infra.repository.entity.PushHistory;

import java.util.List;

/**
 * Service interface for managing listing push operations.
 * Handles pushing listings immediately, scheduling automatic pushes, and recording push history.
 */
public interface PushService {

    /**
     * Push a listing immediately.
     * Can use membership quota or require payment.
     *
     * @param userId The user ID
     * @param request The push request containing listing ID and payment preferences
     * @return PushResponse with result or payment URL if payment required
     */
    PushResponse pushListing(String userId, PushListingRequest request);

    /**
     * Complete push after successful payment.
     * Called from payment callback handler.
     *
     * @param transactionId The completed transaction ID
     * @return PushResponse with result
     */
    PushResponse completePushAfterPayment(String transactionId);

    /**
     * Schedule automatic pushes for a listing.
     * Can use membership quota or require payment.
     *
     * @param userId The user ID
     * @param request The schedule request containing listing ID, time, and total pushes
     * @return PushResponse with schedule result
     */
    PushResponse schedulePush(String userId, SchedulePushRequest request);

    /**
     * Execute all scheduled pushes for the current time.
     * Called by cron job every minute/hour.
     *
     * @return Number of pushes executed
     */
    int executeScheduledPushes();

    /**
     * Cancel a scheduled push.
     *
     * @param userId The user ID
     * @param scheduleId The schedule ID to cancel
     */
    void cancelScheduledPush(String userId, Long scheduleId);

    /**
     * Get push history for a listing.
     *
     * @param listingId The listing ID
     * @return List of push responses
     */
    List<PushResponse> getPushHistory(Long listingId);

    /**
     * Get push history for a listing with pagination.
     *
     * @param listingId The listing ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated push responses
     */
    PageResponse<PushResponse> getPushHistory(Long listingId, int page, int size);

    /**
     * Get push history for all listings owned by a user.
     *
     * @param userId The user ID
     * @return List of push responses for all user's listings
     */
    List<PushResponse> getUserPushHistory(String userId);

    /**
     * Get push history for all listings owned by a user with pagination.
     *
     * @param userId The user ID
     * @param page Page number (1-indexed)
     * @param size Page size
     * @return Paginated push responses for all user's listings
     */
    PageResponse<PushResponse> getUserPushHistory(String userId, int page, int size);

    /**
     * Get push history for a specific listing
     *
     * @param listingId The listing ID
     * @return List of push history records
     */
    List<PushHistory> getPushHistoryByListingId(Long listingId);

    /**
     * Get push history for a specific schedule
     *
     * @param scheduleId The schedule ID
     * @return List of push history records
     */
    List<PushHistory> getPushHistoryByScheduleId(Long scheduleId);

    /**
     * Get push history records for all listings owned by a user.
     *
     * @param userId The user ID
     * @return List of push history entities for all user's listings
     */
    List<PushHistory> getPushHistoryByUserId(String userId);
}
