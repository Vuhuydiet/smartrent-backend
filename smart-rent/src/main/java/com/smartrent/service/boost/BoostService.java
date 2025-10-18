package com.smartrent.service.boost;

import com.smartrent.dto.request.BoostListingRequest;
import com.smartrent.dto.request.ScheduleBoostRequest;
import com.smartrent.dto.response.BoostResponse;

import java.util.List;

public interface BoostService {

    /**
     * Boost a listing immediately
     */
    BoostResponse boostListing(String userId, BoostListingRequest request);

    /**
     * Schedule automatic boosts for a listing
     */
    BoostResponse scheduleBoost(String userId, ScheduleBoostRequest request);

    /**
     * Get boost history for a listing
     */
    List<BoostResponse> getBoostHistory(Long listingId);

    /**
     * Get boost history for a user
     */
    List<BoostResponse> getUserBoostHistory(String userId);

    /**
     * Execute scheduled boosts (called by cron job)
     */
    int executeScheduledBoosts();

    /**
     * Cancel a scheduled boost
     */
    void cancelScheduledBoost(String userId, Long scheduleId);
}

