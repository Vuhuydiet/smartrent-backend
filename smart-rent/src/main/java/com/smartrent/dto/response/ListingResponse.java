package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Listing details response")
public class ListingResponse {
    @Schema(description = "Listing ID", example = "1")
    Long listingId;

    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Q7")
    String title;

    @Schema(description = "Detailed description")
    String description;

    @Schema(description = "User ID of the listing owner", example = "user-123")
    String userId;

    @Schema(description = "Owner's contact phone number for Zalo or other messaging", example = "0912345678")
    String ownerContactPhoneNumber;

    @Schema(description = "Whether the owner's contact phone number has been verified", example = "true")
    Boolean ownerContactPhoneVerified;

    @Schema(description = "Direct Zalo link to contact owner", example = "https://zalo.me/0912345678")
    String ownerZaloLink;

    @Schema(description = "Whether owner has contact available (phone present AND verified)")
    Boolean contactAvailable;

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

    @Schema(description = "Whether listing is a draft (incomplete, not yet published)")
    Boolean isDraft;

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

    @Schema(description = "List of amenities associated with this listing")
    List<AmenityResponse> amenities;

    @Schema(description = "Location-based pricing information for this listing and similar listings in the same area")
    LocationPricingResponse locationPricing;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}