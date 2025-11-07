package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for housing price prediction")
public class HousingPredictorRequest {
    
    @JsonProperty("city")
    @NotBlank(message = "City is required")
    @Schema(description = "City name", example = "Ho Chi Minh", required = true)
    private String city;
    
    @JsonProperty("district")
    @NotBlank(message = "District is required")
    @Schema(description = "District name", example = "District 1", required = true)
    private String district;
    
    @JsonProperty("ward")
    @NotBlank(message = "Ward is required")
    @Schema(description = "Ward name", example = "Ben Nghe Ward", required = true)
    private String ward;
    
    @JsonProperty("property_type")
    @NotBlank(message = "Property type is required")
    @Schema(description = "Type of property", example = "Apartment", allowableValues = {"House", "Apartment", "Villa", "Townhouse"}, required = true)
    private String propertyType;
    
    @JsonProperty("area")
    @NotNull(message = "Area is required")
    @Min(value = 1, message = "Area must be at least 1 square meter")
    @Schema(description = "Property area in square meters", example = "80", minimum = "1", required = true)
    private Integer area;
    
    @JsonProperty("latitude")
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Property latitude coordinate", example = "10.7769", minimum = "-90", maximum = "90", required = true)
    private Double latitude;
    
    @JsonProperty("longitude")
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Property longitude coordinate", example = "106.7009", minimum = "-180", maximum = "180", required = true)
    private Double longitude;
}
