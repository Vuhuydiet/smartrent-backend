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
@Schema(description = "Individual listing pricing information for location comparison")
public class ListingPricingInfo {

    @Schema(description = "Listing ID", example = "123")
    Long listingId;

    @Schema(description = "Listing title", example = "Căn hộ 2 phòng ngủ")
    String title;

    @Schema(description = "Current price", example = "1200000")
    BigDecimal price;

    @Schema(description = "Price unit", example = "MONTH", allowableValues = {"MONTH", "DAY", "YEAR"})
    String priceUnit;

    @Schema(description = "Area in square meters", example = "65.0")
    Float area;

    @Schema(description = "Price per square meter", example = "18461")
    BigDecimal pricePerSqm;

    @Schema(description = "Number of bedrooms", example = "2")
    Integer bedrooms;

    @Schema(description = "Number of bathrooms", example = "1")
    Integer bathrooms;

    @Schema(description = "Product type", example = "APARTMENT")
    String productType;

    @Schema(description = "VIP type", example = "NORMAL")
    String vipType;

    @Schema(description = "Whether this listing is verified", example = "true")
    Boolean verified;
}