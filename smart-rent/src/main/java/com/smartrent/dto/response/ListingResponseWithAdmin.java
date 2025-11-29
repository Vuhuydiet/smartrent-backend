package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Listing details response with admin verification information")
public class ListingResponseWithAdmin {

    @Schema(description = "Listing ID", example = "1")
    Long listingId;

    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Q7")
    String title;

    @Schema(description = "Detailed description")
    String description;

    @Schema(description = "User ID of the listing owner", example = "user-123")
    String userId;

    @Schema(description = "Post date")
    LocalDateTime postDate;

    @Schema(description = "Expiry date")
    LocalDateTime expiryDate;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Whether listing is verified")
    Boolean verified;

    @Schema(description = "Whether listing is being verified")
    Boolean isVerify;

    @Schema(description = "Whether listing is expired")
    Boolean expired;

    @Schema(
        description = "VIP tier: NORMAL, SILVER, GOLD, DIAMOND",
        example = "SILVER",
        allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"}
    )
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

    @Schema(description = "Water price information", example = "50000 VND/month")
    String waterPrice;

    @Schema(description = "Electricity price information", example = "3500 VND/kWh")
    String electricityPrice;

    @Schema(description = "Internet price information", example = "100000 VND/month")
    String internetPrice;

    @Schema(description = "Service fee information", example = "200000 VND/month")
    String serviceFee;

    @Schema(description = "List of amenities associated with this listing")
    List<AmenityResponse> amenities;

    @Schema(description = "Admin verification information")
    AdminVerificationInfo adminVerification;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @Schema(description = "ID of admin who last updated the listing")
    Long updatedBy;
}