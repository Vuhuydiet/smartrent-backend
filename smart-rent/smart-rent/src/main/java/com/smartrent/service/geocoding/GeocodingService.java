package com.smartrent.service.geocoding;

import com.smartrent.dto.response.GeocodingResponse;

/**
 * Service for geocoding operations (converting addresses to geographic coordinates)
 */
public interface GeocodingService {

    /**
     * Convert an address string to geographic coordinates (latitude/longitude)
     *
     * @param address The address to geocode (can be full address or partial)
     * @return GeocodingResponse containing latitude, longitude, and formatted address
     * @throws com.smartrent.infra.exception.AppException if geocoding fails or address not found
     */
    GeocodingResponse geocodeAddress(String address);
}