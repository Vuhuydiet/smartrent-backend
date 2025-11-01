package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * Request DTO for housing price prediction
 * Based on AI service API: /api/v1/house-pricing/get-price-range
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Property information for AI-powered price prediction",
    example = """
        {
          "latitude": 21.0285,
          "longitude": 105.8542,
          "propertyType": "Apartment",
          "city": "Hanoi", 
          "district": "Ba Dinh",
          "ward": "Cong Vi",
          "area": 75.5
        }
        """
)
public class HousingPredictorRequest {

    @Schema(
        description = "GPS latitude coordinate of the property location within Vietnam", 
        example = "21.0285", 
        required = true,
        minimum = "8.0",
        maximum = "23.5"
    )
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(
        description = "GPS longitude coordinate of the property location within Vietnam", 
        example = "105.8542", 
        required = true,
        minimum = "102.0",
        maximum = "110.0"
    )
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(
        description = "Type of property for price prediction", 
        example = "Apartment", 
        required = true, 
        allowableValues = {"Apartment", "House", "Office", "Room"},
        implementation = String.class
    )
    @NotNull(message = "Property type is required")
    @JsonProperty("property_type")
    private String propertyType;

    @Schema(
        description = "Vietnamese city or province name where the property is located", 
        example = "Hanoi", 
        required = true
    )
    @NotNull(message = "City is required")
    private String city;

    @Schema(
        description = "District (Quận/Huyện) name within the specified city", 
        example = "Ba Dinh", 
        required = true
    )
    @NotNull(message = "District is required")
    private String district;

    @Schema(
        description = "Ward (Phường/Xã) name within the specified district", 
        example = "Cong Vi", 
        required = true
    )
    @NotNull(message = "Ward is required")
    private String ward;

    @Schema(
        description = "Property floor area in square meters (must be positive value)", 
        example = "75.5", 
        required = true,
        minimum = "1.0",
        maximum = "10000.0"
    )
    @NotNull(message = "Area is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be positive")
    private Float area;

    @Schema(
        description = "Property listing post date (optional - used for market analysis)", 
        example = "2025-11-01T14:30:22"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime postDate;
}