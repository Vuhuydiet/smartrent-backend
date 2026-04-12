package com.smartrent.service.recommendation;

import com.smartrent.dto.response.RecommendationResponse;

public interface RecommendationService {

    /**
     * Get similar listings based on a target listing (Public, CBF-focused)
     */
    RecommendationResponse getSimilarListings(Long listingId, int topN);

    /**
     * Get personalized feed based on user interactions (Authenticated, Hybrid CF+CBF)
     */
    RecommendationResponse getPersonalizedFeed(String userId, int topN);
}
