package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Criteria for the price-comparables aggregate query. Unlike the open
 * {@code /v1/listings/search} filter, this is a purpose-built, geo-radius query:
 * the caller (the AI price-prediction agent) picks the criteria, and the backend
 * returns deterministic price statistics computed in SQL/Java — never a paginated
 * page of listing cards for the model to eyeball and total up by hand.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Criteria for the price-comparables aggregate query")
public class PriceComparablesRequest {

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Schema(description = "Center latitude of the search radius", example = "21.064731")
    Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Schema(description = "Center longitude of the search radius", example = "105.8346522")
    Double longitude;

    @Schema(description = "Search radius in kilometers (default 2, max 20)", example = "2.0")
    Double radiusKm;

    @NotBlank(message = "productType is required")
    @Schema(description = "Property type", example = "ROOM",
            allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO", "STORE"})
    String productType;

    @Schema(description = "Listing type (default RENT)", example = "RENT",
            allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Price unit to compare within (default MONTH)", example = "MONTH",
            allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Schema(description = "Minimum comparable area in m² (optional)", example = "38.5")
    Float minArea;

    @Schema(description = "Maximum comparable area in m² (optional)", example = "71.5")
    Float maxArea;

    @Schema(description = "Max comparables to sample, nearest first (default 100, max 200)", example = "100")
    Integer limit;
}
