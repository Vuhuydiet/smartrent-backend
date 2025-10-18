package com.smartrent.service.push;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for managing listing push operations.
 * Handles pushing listings at scheduled times and recording push history.
 */
public interface PushService {

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
    List<Object> getPushHistoryByListingId(Long listingId);

    /**
     * Get push history for a specific schedule
     *
     * @param scheduleId The schedule ID
     * @return List of push history records
     */
    List<Object> getPushHistoryByScheduleId(Long scheduleId);
}
