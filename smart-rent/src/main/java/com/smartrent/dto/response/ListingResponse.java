package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Listing details response")
public class ListingResponse {
    @Schema(description = "Listing ID", example = "1")
    Long listingId;

    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Q7")
    String title;

    @Schema(description = "Detailed description")
    String description;

    @Schema(description = "User information of the listing owner")
    UserCreationResponse user;

    @Schema(description = "Post date")
    LocalDateTime postDate;

    @Schema(description = "Expiry date")
    LocalDateTime expiryDate;

    @Schema(description = "Listing type", example = "RENT", allowableValues = {"RENT", "SALE", "SHARE"})
    String listingType;

    @Schema(description = "Whether listing is verified")
    Boolean verified;

    @Schema(description = "Whether listing is expired")
    Boolean expired;

    @Schema(description = "Whether listing is a draft (incomplete, not yet published)")
    Boolean isDraft;

    @Schema(description = "Computed listing status for owner view",
            example = "DISPLAYING",
            allowableValues = {"EXPIRED", "EXPIRING_SOON", "DISPLAYING", "IN_REVIEW", "PENDING_PAYMENT", "REJECTED", "VERIFIED", "DISABLED"})
    String listingStatus;

    @Schema(
        description = "VIP tier: NORMAL, SILVER, GOLD, DIAMOND",
        example = "SILVER",
        allowableValues = {"NORMAL", "SILVER", "GOLD", "DIAMOND"}
    )
    String vipType;

    Long categoryId;

    @Schema(description = "Product type (loại bất động sản)", example = "APARTMENT", allowableValues = {"ROOM", "APARTMENT", "HOUSE", "OFFICE", "STUDIO"})
    String productType;

    BigDecimal price;

    String priceUnit;

    @Schema(description = "Full address information")
    AddressResponse address;

    Float area;

    Integer bedrooms;

    Integer bathrooms;

    String direction;

    String furnishing;

    Integer roomCapacity;

    @Schema(description = "Water price information", example = "50000 VND/month")
    String waterPrice;

    @Schema(description = "Electricity price information", example = "3500 VND/kWh")
    String electricityPrice;

    @Schema(description = "Internet price information", example = "100000 VND/month")
    String internetPrice;

    @Schema(description = "Service fee information", example = "200000 VND/month")
    String serviceFee;

    @Schema(description = "Owner's contact phone number", example = "0901234567")
    String ownerContactPhoneNumber;

    @Schema(description = "Whether the owner's contact phone number has been verified")
    Boolean ownerContactPhoneVerified;

    @Schema(description = "Owner's Zalo link", example = "https://zalo.me/0901234567")
    String ownerZaloLink;

    @Schema(description = "Whether contact information is available")
    Boolean contactAvailable;

    @Schema(description = "List of amenities associated with this listing")
    List<AmenityResponse> amenities;

    @Schema(description = "List of media (images/videos) for this listing, ordered by sort_order")
    List<MediaResponse> media;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}