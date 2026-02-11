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

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Comprehensive location pricing information including statistics and individual listings")
public class LocationPricingResponse {

    @Schema(description = "Ward-level pricing statistics")
    LocationPricingStatistics wardPricing;

    @Schema(description = "District-level pricing statistics")
    LocationPricingStatistics districtPricing;

    @Schema(description = "Province-level pricing statistics")
    LocationPricingStatistics provincePricing;

    @Schema(description = "Project-level pricing statistics (if listing belongs to a project)")
    LocationPricingStatistics projectPricing;

    @Schema(description = "List of similar listings in the same ward (max 10)")
    List<ListingPricingInfo> similarListingsInWard;

    @Schema(description = "List of similar listings in the same district (max 10)")
    List<ListingPricingInfo> similarListingsInDistrict;

    @Schema(description = "Comparison indicator: how this listing's price compares to ward average",
            example = "BELOW_AVERAGE",
            allowableValues = {"WELL_BELOW_AVERAGE", "BELOW_AVERAGE", "AVERAGE", "ABOVE_AVERAGE", "WELL_ABOVE_AVERAGE"})
    String priceComparison;

    @Schema(description = "Percentage difference from ward average price", example = "-15.5")
    Double percentageDifferenceFromAverage;
}