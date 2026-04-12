package com.smartrent.service.recentlyviewed;

import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.RecentlyViewedItemResponse;

import java.util.List;

public interface RecentlyViewedService {
    
    /**
     * Sync local frontend recently viewed history with server.
     * Merges with existing history and updates Redis & MySQL.
     */
    List<RecentlyViewedItemResponse> sync(String userId, RecentlyViewedSyncRequest request);

    /**
     * Retrieve the authenticated user's recently viewed listings.
     */
    List<RecentlyViewedItemResponse> get(String userId);
}
