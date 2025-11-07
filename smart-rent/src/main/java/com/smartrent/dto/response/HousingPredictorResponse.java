package com.smartrent.dto.response;package com.smartrent.dto.response;



import com.fasterxml.jackson.annotation.JsonProperty;import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.media.Schema;import lombok.AllArgsConstructor;

import lombok.AllArgsConstructor;import lombok.Builder;

import lombok.Builder;import lombok.Data;

import lombok.Data;import lombok.NoArgsConstructor;

import lombok.NoArgsConstructor;



@Dataimport java.time.LocalDateTime;

@Builderimport java.util.Map;

@NoArgsConstructor

@AllArgsConstructor/**

@Schema(description = "Response model for housing price prediction") * Response DTO for housing price prediction

public class HousingPredictorResponse { * Based on AI service API response structure

 */

    @JsonProperty("price_range")@Data

    @Schema(description = "Predicted price range for the property")@Builder

    private PriceRange priceRange;@NoArgsConstructor

@AllArgsConstructor

    @JsonProperty("location")@Schema(

    @Schema(description = "Location description", example = "My Tho, Tien Giang")    description = "AI-powered housing price prediction results with market analysis",

    private String location;    example = """

        {

    @JsonProperty("property_type")          "address": "Ba Dinh, Hanoi",

    @Schema(description = "Type of property", example = "House")          "propertyType": "Apartment", 

    private String propertyType;          "predictedPrice": 32.04,

          "priceRange": {

    @JsonProperty("currency")            "minPrice": 28.85,

    @Schema(description = "Currency code", example = "VND")            "maxPrice": 35.23

    private String currency;          },

          "confidence": 0.89,

    @Schema(description = "Calculated predicted price (average of min/max)")          "currency": "VND_millions",

    private Double predictedPrice;          "timestamp": "2025-11-01T14:30:22.123",

          "modelVersion": "ai-v2.1.0",

    @Data          "metadata": {

    @Builder            "calculation_time_ms": 245,

    @NoArgsConstructor            "location": "21.0285,105.8542",

    @AllArgsConstructor            "prediction_method": "neural_network"

    @Schema(description = "Price range information")          }

    public static class PriceRange {        }

        @JsonProperty("min")        """

        @Schema(description = "Minimum predicted price", example = "9464324"))

        private Double minPrice;public class HousingPredictorResponse {



        @JsonProperty("max")    @Schema(

        @Schema(description = "Maximum predicted price", example = "43084827")        description = "Formatted property address (district, city)", 

        private Double maxPrice;        example = "Ba Dinh, Hanoi"

    }    )

}    private String address;

    @Schema(
        description = "Type of property that was analyzed", 
        example = "Apartment"
    )
    private String propertyType;

    @Schema(
        description = "AI-predicted market price in millions VND", 
        example = "32.04"
    )
    private Double predictedPrice;

    @Schema(
        description = "Price range analysis with min/max estimates"
    )
    private PriceRange priceRange;

    @Schema(
        description = "AI model confidence score (0.0 = low confidence, 1.0 = high confidence)", 
        example = "0.89",
        minimum = "0.0",
        maximum = "1.0"
    )
    private Double confidence;

    @Schema(
        description = "Currency denomination for all price values", 
        example = "VND_millions"
    )
    private String currency;

    @Schema(
        description = "Timestamp when the prediction was generated", 
        example = "2024-01-15T10:30:00"
    )
    private LocalDateTime timestamp;

    @Schema(
        description = "Version of the AI model used for prediction", 
        example = "v1.2.3"
    )
    private String modelVersion;

    @Schema(
        description = "Additional metadata about the prediction analysis and model performance"
    )
    private Map<String, Object> metadata;

    /**
     * Nested class representing price range analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Price range analysis with minimum and maximum estimated values")
    public static class PriceRange {
        
        @Schema(
            description = "Maximum estimated price in millions VND", 
            example = "38.7"
        )
        private Double maxPrice;

        @Schema(
            description = "Minimum estimated price in millions VND", 
            example = "25.4"
        )
        private Double minPrice;
    }
}