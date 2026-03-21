package com.smartrent.service.analytics;

import com.smartrent.dto.response.ListingAnalyticsResponse;
import com.smartrent.dto.response.OwnerListingsAnalyticsResponse;
import com.smartrent.dto.response.OwnerSavedListingsAnalyticsResponse;
import com.smartrent.dto.response.SavedListingsTrendResponse;

public interface AnalyticsService {

    ListingAnalyticsResponse getListingAnalytics(Long listingId, String ownerId);

    OwnerListingsAnalyticsResponse getOwnerListingsAnalytics(String ownerId);

    SavedListingsTrendResponse getSavedListingTrend(Long listingId, String ownerId);

    OwnerSavedListingsAnalyticsResponse getOwnerSavedListingsAnalytics(String ownerId);
}
