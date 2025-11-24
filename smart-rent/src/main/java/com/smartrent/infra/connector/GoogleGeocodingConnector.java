package com.smartrent.infra.connector;

import com.smartrent.infra.connector.model.GoogleGeocodingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for Google Geocoding API
 * Documentation: https://developers.google.com/maps/documentation/geocoding/overview
 */
@FeignClient(
    name = "google-geocoding-client",
    url = "${feign.client.config.google-maps.url}"
)
public interface GoogleGeocodingConnector {

    /**
     * Convert address string to geographic coordinates (latitude/longitude)
     *
     * @param address The address to geocode
     * @param apiKey Google Maps API key
     * @return Geocoding response containing coordinates and formatted address
     */
    @GetMapping(value = "/geocode/json")
    GoogleGeocodingResponse geocode(
        @RequestParam("address") String address,
        @RequestParam("key") String apiKey
    );
}