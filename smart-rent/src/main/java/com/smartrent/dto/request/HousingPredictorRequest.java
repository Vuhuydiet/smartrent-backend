package com.smartrent.dto.request;package com.smartrent.dto.request;



import com.fasterxml.jackson.annotation.JsonProperty;import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.media.Schema;import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;import jakarta.validation.constraints.DecimalMax;

import jakarta.validation.constraints.Positive;import jakarta.validation.constraints.DecimalMin;

import lombok.AllArgsConstructor;import jakarta.validation.constraints.NotNull;

import lombok.Builder;

import lombok.Data;import java.time.LocalDateTime;

import lombok.NoArgsConstructor;import lombok.AllArgsConstructor;

import lombok.Builder;

@Dataimport lombok.Data;

@Builderimport lombok.NoArgsConstructor;

@NoArgsConstructor

@AllArgsConstructor

@Schema(description = "Request model for housing price prediction")

public class HousingPredictorRequest {/**

 * Request DTO for housing price prediction

    @NotBlank(message = "City is required") * Based on AI service API: /api/v1/house-pricing/get-price-range

    @JsonProperty("city") */

    @Schema(description = "City name where the property is located", example = "Tien Giang")@Data

    private String city;@Builder

@NoArgsConstructor

    @NotBlank(message = "District is required")@AllArgsConstructor

    @JsonProperty("district")@Schema(

    @Schema(description = "District name where the property is located", example = "My Tho")    description = "Property information for AI-powered price prediction",

    private String district;    example = """

        {

    @NotBlank(message = "Ward is required")          "latitude": 21.0285,

    @JsonProperty("ward")          "longitude": 105.8542,

    @Schema(description = "Ward name where the property is located", example = "Ward 1")          "propertyType": "Apartment",

    private String ward;          "city": "Hanoi", 

          "district": "Ba Dinh",

    @NotBlank(message = "Property type is required")          "ward": "Cong Vi",

    @JsonProperty("property_type")          "area": 75.5

    @Schema(description = "Type of property (House, Apartment, etc.)", example = "House")        }

    private String propertyType;        """

)

    @NotNull(message = "Area is required")public class HousingPredictorRequest {

    @Positive(message = "Area must be positive")

    @JsonProperty("area")    @Schema(

    @Schema(description = "Property area in square meters", example = "60")        description = "GPS latitude coordinate of the property location within Vietnam", 

    private Double area;        example = "21.0285", 

        required = true,

    @NotNull(message = "Latitude is required")        minimum = "8.0",

    @JsonProperty("latitude")        maximum = "23.5"

    @Schema(description = "Latitude coordinate of the property", example = "10.3499")    )

    private Double latitude;    @NotNull(message = "Latitude is required")

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")

    @NotNull(message = "Longitude is required")    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")

    @JsonProperty("longitude")    private Double latitude;

    @Schema(description = "Longitude coordinate of the property", example = "106.3597")

    private Double longitude;    @Schema(

}        description = "GPS longitude coordinate of the property location within Vietnam", 
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