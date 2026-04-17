package com.smartrent.service.recommendation;

import com.smartrent.dto.response.RecommendationResponse;

public interface RecommendationService {

    /**
     * Get similar listings based on a target listing.
     * If userId is provided, results are slightly personalized (60% similar, 40% user preference).
     * Public access — userId is optional (null for anonymous).
     */
    RecommendationResponse getSimilarListings(Long listingId, int topN, String userId);

    /**
     * Get personalized feed based on user interactions (Authenticated, Hybrid CF+CBF)
     */
    RecommendationResponse getPersonalizedFeed(String userId, int topN);
}
