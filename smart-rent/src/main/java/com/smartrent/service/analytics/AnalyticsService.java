package com.smartrent.service.analytics;

import com.smartrent.dto.response.ListingAnalyticsResponse;
import com.smartrent.dto.response.OwnerListingsAnalyticsResponse;

public interface AnalyticsService {

    ListingAnalyticsResponse getListingAnalytics(Long listingId, String ownerId);

    OwnerListingsAnalyticsResponse getOwnerListingsAnalytics(String ownerId);
}
