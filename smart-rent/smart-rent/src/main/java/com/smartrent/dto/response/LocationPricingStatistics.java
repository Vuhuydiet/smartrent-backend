package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pricing statistics for listings in the same location")
public class LocationPricingStatistics {

    @Schema(description = "Location type", example = "WARD", allowableValues = {"WARD", "DISTRICT", "PROVINCE", "PROJECT"})
    String locationType;

    @Schema(description = "Location ID (ward ID, district ID, province ID, or project ID)")
    Integer locationId;

    @Schema(description = "Location name", example = "Phường Bến Nghé")
    String locationName;

    @Schema(description = "Total number of listings in this location", example = "45")
    Integer totalListings;

    @Schema(description = "Average price in this location", example = "1500000")
    BigDecimal averagePrice;

    @Schema(description = "Minimum price in this location", example = "800000")
    BigDecimal minPrice;

    @Schema(description = "Maximum price in this location", example = "3500000")
    BigDecimal maxPrice;

    @Schema(description = "Median price in this location", example = "1400000")
    BigDecimal medianPrice;

    @Schema(description = "Price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Schema(description = "Product type for this pricing data", example = "APARTMENT")
    String productType;

    @Schema(description = "Average area in square meters", example = "65.5")
    Double averageArea;

    @Schema(description = "Average price per square meter", example = "22900")
    BigDecimal averagePricePerSqm;
}