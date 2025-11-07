package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response containing housing price prediction results")
public class HousingPredictorResponse {
    
    @JsonProperty("price_range")
    @Schema(description = "Predicted price range for the property")
    private PriceRange priceRange;
    
    @JsonProperty("location")
    @Schema(description = "Formatted location string", example = "District 1, Ho Chi Minh")
    private String location;
    
    @JsonProperty("property_type")
    @Schema(description = "Type of the property", example = "Apartment")
    private String propertyType;
    
    @JsonProperty("currency")
    @Schema(description = "Currency code for the price", example = "VND")
    private String currency;
    
    @Getter
    @Setter
    @Schema(description = "Price range with minimum and maximum values")
    public static class PriceRange {
        
        @JsonProperty("min")
        @Schema(description = "Minimum predicted price", example = "19882833")
        private Long min;
        
        @JsonProperty("max")
        @Schema(description = "Maximum predicted price", example = "66528144")
        private Long max;
    }
}
