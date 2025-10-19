package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Amenity details response")
public class AmenityResponse {
    @Schema(description = "Amenity ID", example = "1")
    Long amenityId;

    @Schema(description = "Amenity name", example = "Điều hòa")
    String name;

    @Schema(description = "Icon reference", example = "ac-icon")
    String icon;

    @Schema(description = "Description of the amenity")
    String description;

    @Schema(description = "Amenity category", example = "BASIC", allowableValues = {"BASIC", "CONVENIENCE", "SECURITY", "ENTERTAINMENT", "TRANSPORT"})
    String category;

    @Schema(description = "Whether the amenity is active", example = "true")
    Boolean isActive;
}