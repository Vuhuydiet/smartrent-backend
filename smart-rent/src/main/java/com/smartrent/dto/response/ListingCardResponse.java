package com.smartrent.dto.response;

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

/**
 * Minimal listing DTO for public-facing card display (home page, seller profile).
 * Contains only the fields required by PropertyCard on the frontend.
 * For full listing details, see ListingResponse.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListingCardResponse {

    Long listingId;
    String title;
    String description;
    BigDecimal price;
    String priceUnit;
    Float area;
    Integer bedrooms;
    Integer bathrooms;
    Boolean verified;
    String vipType;
    String productType;
    String furnishing;
    String direction;
    Integer roomCapacity;
    LocalDateTime postDate;

    AddressCard address;
    List<MediaCard> media;
    UserCard user;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AddressCard {
        String fullNewAddress;
        String fullAddress;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MediaCard {
        String mediaType;
        String url;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserCard {
        String userId;
        String firstName;
        String lastName;
        String contactPhoneNumber;
        Boolean contactPhoneVerified;
        String avatarUrl;
        Boolean isBroker;
        String brokerVerificationStatus;
    }
}
