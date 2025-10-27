package com.smartrent.dto.request;

import jakarta.validation.Valid;
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
public class ListingCreationRequest {

    @NotBlank
    @Size(max = 200)
    String title;

    @NotBlank
    String description;

    @NotNull
    String userId;

    LocalDateTime expiryDate;

    @NotBlank
    String listingType;


    Boolean verified;
    Boolean isVerify;
    Boolean expired;

    @NotBlank
    String vipType;

    @NotNull
    Long categoryId;

    @NotBlank
    String productType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal price;

    @NotBlank
    String priceUnit;

    @Valid
    @NotNull(message = "Address information is required")
    AddressCreationRequest address;

    @DecimalMin(value = "0.0", inclusive = false)
    Float area;

    @Min(0)
    Integer bedrooms;

    @Min(0)
    Integer bathrooms;

    String direction;

    String furnishing;

    String propertyType;

    @Min(0)
    Integer roomCapacity;

    Set<Long> amenityIds;

    /**
     * List of media IDs to attach to this listing
     * Media must be already uploaded and belong to the same user
     * Optional: If not provided, listing will be created without media
     */
    Set<Long> mediaIds;
}