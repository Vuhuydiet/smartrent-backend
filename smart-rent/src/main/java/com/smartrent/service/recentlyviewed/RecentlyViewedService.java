package com.smartrent.service.recentlyviewed;

import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.RecentlyViewedItemResponse;

import java.util.List;

/**
 * Service for managing recently viewed listings using Redis
 */
public interface RecentlyViewedService {

    /**
     * Sync recently viewed listings from client with server data
     * @param request Request containing client's recently viewed listings
     * @return Merged list of recently viewed listings sorted by most recent
     */
    List<RecentlyViewedItemResponse> syncRecentlyViewed(RecentlyViewedSyncRequest request);

    /**
     * Get user's recently viewed listings from Redis
     * @return List of recently viewed listings sorted by most recent (up to 20 listings)
     */
    List<RecentlyViewedItemResponse> getRecentlyViewed();

    /**
     * Get user's recently viewed listings from Redis by userId (for internal usage like recommendations)
     * @return List of recently viewed listings sorted by most recent (up to 20 listings)
     */
    List<RecentlyViewedItemResponse> getRecentlyViewed(String userId);

    /**
     * Ordered recently-viewed listing IDs (most recent first) straight from the
     * Redis ZSET, WITHOUT hydrating listing details. For internal callers (e.g.
     * recommendations) that only need the IDs and their recency order — avoids
     * the getDisplayingListingsByIds full-DTO build of up to 20 listings.
     * @return listing IDs, most recent first (empty if none)
     */
    List<Long> getRecentlyViewedIds(String userId);
}