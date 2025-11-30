package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request to generate a human-friendly listing description using AI")
public class ListingDescriptionRequest {
    @Schema(description = "Category of the listing", example = "CHO_THUE")
    String category;

    @Schema(description = "Property type", example = "APARTMENT")
    String propertyType;

    @Schema(description = "Price (must be > 0)", example = "12000000")
    java.math.BigDecimal price;

    @Schema(description = "Price unit: MONTH or YEAR", example = "MONTH")
    String priceUnit;

    @Schema(description = "Address information")
    AddressText addressText;

    @Schema(description = "Area in square meters", example = "78.5")
    Float area;

    @Schema(description = "Number of bedrooms", example = "2")
    @Min(0)
    Integer bedrooms;

    @Schema(description = "Number of bathrooms", example = "1")
    @Min(0)
    Integer bathrooms;

    @Schema(description = "Direction/orientation", example = "Đông Nam")
    String direction;

    @Schema(description = "Furnishing status", example = "SEMI_FURNISHED")
    String furnishing;

    @Schema(description = "List of amenities", example = "[\"Điều hòa\", \"Tủ lạnh\", \"Máy giặt\"]")
    java.util.List<String> amenities;

    @Schema(description = "Water price", example = "20000 VND/m3")
    String waterPrice;

    @Schema(description = "Electricity price", example = "3500 VND/kWh")
    String electricityPrice;

    @Schema(description = "Internet price", example = "100000 VND/tháng")
    String internetPrice;

    @Schema(description = "Service fee", example = "500000 VND/tháng")
    String serviceFee;

    @NotNull
    @Schema(description = "Tone for the generated description (short, friendly, professional)", example = "friendly")
    String tone;

    @Schema(description = "Maximum length of description in words", example = "50")
    Integer maxWords;

    @Schema(description = "Minimum length of description in words", example = "20")
    Integer minWords;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AddressText {
        @Schema(description = "New address format", example = "123 Đường Láng, Đống Đa, Hà Nội")
        String newAddress;

        @Schema(description = "Legacy address format (optional)", example = "Số 123, Đường Láng")
        String legacy;
    }
}
