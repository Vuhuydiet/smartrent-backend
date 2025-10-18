package com.smartrent.service.push;

import com.smartrent.dto.request.BoostListingRequest;
import com.smartrent.dto.request.ScheduleBoostRequest;
import com.smartrent.dto.response.BoostResponse;
import com.smartrent.infra.repository.entity.PushHistory;
import com.smartrent.infra.repository.entity.PushSchedule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service interface for managing listing push/boost operations.
 * Handles pushing listings (boost) immediately, scheduling automatic pushes, and recording push history.
 */
public interface PushService {

    /**
     * Boost (push) a listing immediately.
     * Can use membership quota or require payment.
     *
     * @param userId The user ID
     * @param request The boost request containing listing ID and payment preferences
     * @return BoostResponse with result or payment URL if payment required
     */
    BoostResponse boostListing(String userId, BoostListingRequest request);

    /**
     * Complete boost after successful payment.
     * Called from payment callback handler.
     *
     * @param transactionId The completed transaction ID
     * @return BoostResponse with result
     */
    BoostResponse completeBoostAfterPayment(String transactionId);

    /**
     * Schedule automatic boosts for a listing.
     * Can use membership quota or require payment.
     *
     * @param userId The user ID
     * @param request The schedule request containing listing ID, time, and total pushes
     * @return BoostResponse with schedule result
     */
    BoostResponse scheduleBoost(String userId, ScheduleBoostRequest request);

    /**
     * Execute all scheduled boosts for the current time.
     * Called by cron job every minute/hour.
     *
     * @return Number of boosts executed
     */
    int executeScheduledBoosts();

    /**
     * Cancel a scheduled boost.
     *
     * @param userId The user ID
     * @param scheduleId The schedule ID to cancel
     */
    void cancelScheduledBoost(String userId, Long scheduleId);

    /**
     * Get boost/push history for a listing.
     *
     * @param listingId The listing ID
     * @return List of boost responses
     */
    List<BoostResponse> getBoostHistory(Long listingId);

    /**
     * Get boost/push history for all listings owned by a user.
     *
     * @param userId The user ID
     * @return List of boost responses for all user's listings
     */
    List<BoostResponse> getUserBoostHistory(String userId);

    /**
     * Create a push schedule for a listing.
     * Validates that listing doesn't already have an active schedule.
     *
     * @param listingId The ID of the listing to schedule
     * @param scheduledTime The time of day to push (e.g., 09:00, 15:00)
     * @param endTime The expiration time for this schedule
     * @return The created PushSchedule
     * @throws IllegalStateException if listing already has an active schedule
     * @throws IllegalArgumentException if listing doesn't exist
     */
    PushSchedule createSchedule(Long listingId, LocalTime scheduledTime, LocalDateTime endTime);

    /**
     * Push a listing by updating its pushed_at timestamp and recording the operation in history.
     * This is the main operation that gets triggered by the scheduler.
     *
     * @param scheduleId The ID of the schedule triggering the push
     * @param listingId The ID of the listing to push
     * @param pushTime The time to set as pushed_at
     * @return true if push was successful, false otherwise
     */
    boolean pushListing(Long scheduleId, Long listingId, LocalDateTime pushTime);

    /**
     * Process all active schedules for the current hour.
     * This method is called by the scheduler at the start of each hour.
     *
     * @param currentTime The current time (used to find schedules)
     * @return Number of listings successfully pushed
     */
    int processScheduledPushes(LocalDateTime currentTime);

    /**
     * Mark expired schedules as EXPIRED.
     * This method should be run periodically to clean up old schedules.
     *
     * @param currentTime The current time
     * @return Number of schedules marked as expired
     */
    int expireOldSchedules(LocalDateTime currentTime);

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
}
