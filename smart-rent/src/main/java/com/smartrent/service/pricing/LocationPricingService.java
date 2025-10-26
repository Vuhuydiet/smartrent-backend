package com.smartrent.service.pricing;

import com.smartrent.dto.response.LocationPricingResponse;
import com.smartrent.infra.repository.entity.Listing;

public interface LocationPricingService {
    /**
     * Get location-based pricing information for a listing
     * @param listing The listing to get pricing information for
     * @return LocationPricingResponse containing statistics and similar listings
     */
    LocationPricingResponse getLocationPricing(Listing listing, Integer wardId, Integer districtId, Integer provinceId);
}