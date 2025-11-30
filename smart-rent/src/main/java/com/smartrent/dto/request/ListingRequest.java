package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update or filter listings")
public class ListingRequest {
    @Size(max = 200)
    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Q7")
    String title;

    @Size(max = 10000)
    @Schema(description = "Detailed description", example = "Căn hộ đẹp, view sông")
    String description;

    @NotNull
    @Schema(description = "User ID", example = "1", required = true)
    Long userId;

    @Schema(description = "Expiry date of the listing")
    LocalDateTime expiryDate;

    @Pattern(regexp = "RENT|SALE|SHARE", message = "listingType must be RENT, SALE, or SHARE")
    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Whether listing is verified")
    Boolean verified;

    @Schema(description = "Whether listing is being verified")
    Boolean isVerify;

    @Schema(description = "Whether listing is expired")
    Boolean expired;

    @Pattern(regexp = "NORMAL|SILVER|GOLD|DIAMOND", message = "vipType must be NORMAL, SILVER, GOLD, or DIAMOND")
    @Schema(
        description = "VIP tier: NORMAL (2.7k/day), SILVER (50k/day), GOLD (110k/day), DIAMOND (280k/day)",
        example = "SILVER",
        allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"}
    )
    String vipType;

    @NotNull
    Long categoryId;

    @Pattern(regexp = "ROOM|APARTMENT|HOUSE|OFFICE|STUDIO", message = "productType must be ROOM, APARTMENT, HOUSE, OFFICE, or STUDIO")
    String productType;

    @DecimalMin(value = "0.0", inclusive = false, message = "price must be positive")
    BigDecimal price;

    @Pattern(regexp = "MONTH|DAY|YEAR", message = "priceUnit must be MONTH, DAY, or YEAR")
    String priceUnit;

    @NotNull
    Long addressId;

    @DecimalMin(value = "0.0", inclusive = false, message = "area must be positive")
    Float area;

    @Min(0)
    Integer bedrooms;

    @Min(0)
    Integer bathrooms;

    @Pattern(regexp = "NORTH|SOUTH|EAST|WEST|NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST", message = "direction must be a valid value")
    String direction;

    @Pattern(regexp = "FULLY_FURNISHED|SEMI_FURNISHED|UNFURNISHED", message = "furnishing must be a valid value")
    String furnishing;

    @Min(0)
    Integer roomCapacity;

    Set<Long> amenityIds;
}
