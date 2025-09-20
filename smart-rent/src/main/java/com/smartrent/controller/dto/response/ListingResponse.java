package com.smartrent.controller.dto.response;

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
public class ListingResponse {
    Long listingId;

    String title;

    String description;

    String userId;

    LocalDateTime postDate;

    LocalDateTime expiryDate;

    String listingType;


    Boolean verified;
    Boolean isVerify;
    Boolean expired;

    String vipType;

    Long categoryId;

    String productType;

    BigDecimal price;

    String priceUnit;

    Long addressId;

    Float area;

    Integer bedrooms;

    Integer bathrooms;

    String direction;

    String furnishing;

    String propertyType;

    Integer roomCapacity;

    Set<Long> amenityIds;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}