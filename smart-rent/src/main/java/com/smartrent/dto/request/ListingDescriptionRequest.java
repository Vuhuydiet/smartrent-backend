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
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to generate a human-friendly listing description using AI")
public class ListingDescriptionRequest {
    @Schema(description = "Optional title for the listing", example = "Căn hộ 2PN view sông")
    String title;

    @Schema(description = "Full address text if available", example = "123 Đường Láng, Đống Đa, Hà Nội")
    String addressText;

    @Schema(description = "Number of bedrooms", example = "2")
    @Min(0)
    Integer bedrooms;

    @Schema(description = "Number of bathrooms", example = "1")
    @Min(0)
    Integer bathrooms;

    @Schema(description = "Area in square meters", example = "78.5")
    Float area;

    @Schema(description = "Price (numeric) - currency not enforced here", example = "12000000")
    java.math.BigDecimal price;

    @Schema(description = "Price unit, e.g. MONTH/DAY/YEAR", example = "MONTH")
    String priceUnit;

    @Schema(description = "Furnishing status", example = "SEMI_FURNISHED")
    String furnishing;

    @Schema(description = "Property type", example = "APARTMENT")
    String propertyType;

    @Schema(description = "Optional amenity ids (if names are not provided)")
    Set<Long> amenityIds;

    @NotNull
    @Schema(description = "Tone for the generated description (short, friendly, professional)", example = "friendly")
    String tone;

    @Schema(description = "Maximum length of description in words (optional)", example = "50")
    Integer maxWords;
}
