package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for geocoding (address to coordinates conversion)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geocoding response containing geographic coordinates for an address")
public class GeocodingResponse {

    @Schema(
        description = "Latitude in decimal degrees",
        example = "21.0285",
        required = true
    )
    private Double latitude;

    @Schema(
        description = "Longitude in decimal degrees",
        example = "105.8542",
        required = true
    )
    private Double longitude;

    @Schema(
        description = "Formatted full address returned by Google",
        example = "Hanoi, Hoàn Kiếm, Hanoi, Vietnam",
        required = true
    )
    private String formattedAddress;

    @Schema(
        description = "Original address query string",
        example = "Hanoi, Vietnam"
    )
    private String originalAddress;

    @Schema(
        description = "Type of location accuracy (ROOFTOP, RANGE_INTERPOLATED, GEOMETRIC_CENTER, APPROXIMATE)",
        example = "APPROXIMATE"
    )
    private String locationType;

    @Schema(
        description = "Place ID from Google Maps",
        example = "ChIJoRyG2ZurNTERqRfKcnt_iOk"
    )
    private String placeId;
}