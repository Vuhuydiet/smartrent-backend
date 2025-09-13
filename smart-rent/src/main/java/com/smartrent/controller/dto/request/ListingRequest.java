package com.smartrent.controller.dto.request;

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
public class ListingRequest {
    @Size(max = 200)
    String title;

    @Size(max = 10000)
    String description;

    @NotNull
    Long userId;

    LocalDateTime expiryDate;

    @Pattern(regexp = "RENT|SALE|SHARE", message = "listingType must be RENT, SALE, or SHARE")
    String listingType;

    Boolean verified;
    Boolean expired;

    @Pattern(regexp = "NORMAL|VIP|PREMIUM", message = "vipType must be NORMAL, VIP, or PREMIUM")
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

    @Pattern(regexp = "APARTMENT|HOUSE|ROOM|STUDIO|OFFICE", message = "propertyType must be a valid value")
    String propertyType;

    @Min(0)
    Integer roomCapacity;

    Set<Long> amenityIds;
}
