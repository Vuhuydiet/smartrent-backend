package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Price statistics for a listing")
public class PriceStatisticsResponse {

    @Schema(description = "Minimum price in history", example = "8000000.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price in history", example = "18000000.00")
    private BigDecimal maxPrice;

    @Schema(description = "Average price in history", example = "12500000.00")
    private BigDecimal avgPrice;

    @Schema(description = "Total number of price changes", example = "5")
    private int totalChanges;

    @Schema(description = "Number of price increases", example = "3")
    private int priceIncreases;

    @Schema(description = "Number of price decreases", example = "2")
    private int priceDecreases;
}