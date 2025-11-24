package com.smartrent.infra.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from Google Geocoding API
 * Documentation: https://developers.google.com/maps/documentation/geocoding/requests-geocoding#GeocodingResponses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleGeocodingResponse {

    /**
     * Status of the request
     * Values: "OK", "ZERO_RESULTS", "OVER_QUERY_LIMIT", "REQUEST_DENIED", "INVALID_REQUEST", "UNKNOWN_ERROR"
     */
    private String status;

    /**
     * More detailed information about the reasons behind the given status code
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Array of geocoding results
     */
    private List<GeocodingResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodingResult {
        /**
         * Formatted address string
         */
        @JsonProperty("formatted_address")
        private String formattedAddress;

        /**
         * Geometry information including location coordinates
         */
        private Geometry geometry;

        /**
         * Unique identifier for the place
         */
        @JsonProperty("place_id")
        private String placeId;

        /**
         * Address components (street, city, country, etc.)
         */
        @JsonProperty("address_components")
        private List<AddressComponent> addressComponents;

        /**
         * Array indicating the type of the returned result
         */
        private List<String> types;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Geometry {
        /**
         * Latitude and longitude coordinates
         */
        private Location location;

        /**
         * Type of location (e.g., "ROOFTOP", "RANGE_INTERPOLATED", "GEOMETRIC_CENTER", "APPROXIMATE")
         */
        @JsonProperty("location_type")
        private String locationType;

        /**
         * Bounding box which can fully contain the returned result
         */
        private Bounds viewport;

        /**
         * Recommended viewport for displaying the result (optional)
         */
        private Bounds bounds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        /**
         * Latitude in decimal degrees
         */
        @JsonProperty("lat")
        private Double latitude;

        /**
         * Longitude in decimal degrees
         */
        @JsonProperty("lng")
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bounds {
        /**
         * Northeast corner of the bounding box
         */
        private Location northeast;

        /**
         * Southwest corner of the bounding box
         */
        private Location southwest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressComponent {
        /**
         * Full text description or name of the address component
         */
        @JsonProperty("long_name")
        private String longName;

        /**
         * Abbreviated textual name for the address component
         */
        @JsonProperty("short_name")
        private String shortName;

        /**
         * Array indicating the type of the address component
         */
        private List<String> types;
    }
}