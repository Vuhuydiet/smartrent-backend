package com.smartrent.service.analytics;

import com.smartrent.dto.response.ListingAnalyticsResponse;
import com.smartrent.dto.response.OwnerListingsAnalyticsResponse;
import com.smartrent.dto.response.OwnerSavedListingsAnalyticsResponse;
import com.smartrent.dto.response.SavedListingsTrendResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AnalyticsService {

    ListingAnalyticsResponse getListingAnalytics(Long listingId, String ownerId, LocalDateTime since);

    OwnerListingsAnalyticsResponse getOwnerListingsAnalytics(String ownerId, String search, Pageable pageable);

    SavedListingsTrendResponse getSavedListingTrend(Long listingId, String ownerId, LocalDateTime since);

    OwnerSavedListingsAnalyticsResponse getOwnerSavedListingsAnalytics(String ownerId, String search, Pageable pageable);
}
