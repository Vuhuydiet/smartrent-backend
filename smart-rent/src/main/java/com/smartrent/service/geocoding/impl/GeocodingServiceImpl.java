package com.smartrent.service.geocoding.impl;

import com.smartrent.dto.response.GeocodingResponse;
import com.smartrent.infra.connector.GoogleGeocodingConnector;
import com.smartrent.infra.connector.model.GoogleGeocodingResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Implementation of GeocodingService using Google Geocoding API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingServiceImpl implements GeocodingService {

    private final GoogleGeocodingConnector googleGeocodingConnector;

    @Value("${google.maps.api-key}")
    private String googleMapsApiKey;

    /**
     * {@inheritDoc}
     *
     * Results are cached for 24 hours to reduce API calls and costs
     */
    @Override
    @Cacheable(value = "locationCache", key = "#address", unless = "#result == null")
    public GeocodingResponse geocodeAddress(String address) {
        log.info("Geocoding address: {}", address);

        if (address == null || address.trim().isEmpty()) {
            log.error("Address is null or empty");
            throw new AppException(DomainCode.EMPTY_INPUT, "Address cannot be empty");
        }

        try {
            // Call Google Geocoding API
            GoogleGeocodingResponse apiResponse = googleGeocodingConnector.geocode(
                    address.trim(),
                    googleMapsApiKey
            );

            // Check response status
            if (!"OK".equals(apiResponse.getStatus())) {
                log.error("Geocoding failed with status: {} - {}",
                        apiResponse.getStatus(),
                        apiResponse.getErrorMessage());

                return handleGeocodingError(apiResponse.getStatus(), apiResponse.getErrorMessage(), address);
            }

            // Check if results are empty
            if (apiResponse.getResults() == null || apiResponse.getResults().isEmpty()) {
                log.warn("No results found for address: {}", address);
                throw new AppException(DomainCode.ADDRESS_NOT_FOUND, "No location found for the provided address");
            }

            // Get the first (most relevant) result
            GoogleGeocodingResponse.GeocodingResult result = apiResponse.getResults().get(0);
            GoogleGeocodingResponse.Location location = result.getGeometry().getLocation();

            log.info("Successfully geocoded address '{}' to coordinates: lat={}, lng={}",
                    address, location.getLatitude(), location.getLongitude());

            // Build response
            return GeocodingResponse.builder()
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .formattedAddress(result.getFormattedAddress())
                    .originalAddress(address)
                    .locationType(result.getGeometry().getLocationType())
                    .placeId(result.getPlaceId())
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during geocoding for address: {}", address, e);
            throw new AppException(DomainCode.UNKNOWN_ERROR, "Failed to geocode address: " + e.getMessage());
        }
    }

    /**
     * Handle different error statuses from Google Geocoding API
     */
    private GeocodingResponse handleGeocodingError(String status, String errorMessage, String address) {
        switch (status) {
            case "ZERO_RESULTS":
                log.warn("No results found for address: {}", address);
                throw new AppException(DomainCode.ADDRESS_NOT_FOUND, "No location found for the provided address");
            case "OVER_QUERY_LIMIT":
                log.error("Google Maps API quota exceeded");
                throw new AppException(DomainCode.TOO_MANY_REQUESTS,
                        "Geocoding service temporarily unavailable. Please try again later.");
            case "REQUEST_DENIED":
                log.error("Google Maps API request denied: {}", errorMessage);
                throw new AppException(DomainCode.EXTERNAL_SERVICE_ERROR,
                        "Geocoding service access denied");
            case "INVALID_REQUEST":
                log.error("Invalid geocoding request for address: {}", address);
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Invalid address format");
            default:
                log.error("Unknown geocoding error: {} - {}", status, errorMessage);
                throw new AppException(DomainCode.EXTERNAL_SERVICE_ERROR,
                        "Failed to geocode address");
        }
    }
}
