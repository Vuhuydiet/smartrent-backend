package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for housing price prediction
 * Based on AI service API response structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "AI-powered housing price prediction results with market analysis",
    example = """
        {
          "address": "Ba Dinh, Hanoi",
          "propertyType": "Apartment", 
          "predictedPrice": 32.04,
          "priceRange": {
            "minPrice": 28.85,
            "maxPrice": 35.23
          },
          "confidence": 0.89,
          "currency": "VND_millions",
          "timestamp": "2025-11-01T14:30:22.123",
          "modelVersion": "ai-v2.1.0",
          "metadata": {
            "calculation_time_ms": 245,
            "location": "21.0285,105.8542",
            "prediction_method": "neural_network"
          }
        }
        """
)
public class HousingPredictorResponse {

    @Schema(
        description = "Formatted property address (district, city)", 
        example = "Ba Dinh, Hanoi"
    )
    private String address;

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