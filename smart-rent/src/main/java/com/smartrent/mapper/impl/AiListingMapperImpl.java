package com.smartrent.mapper.impl;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.enums.HousingPropertyType;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.Amenity;
import com.smartrent.mapper.AiListingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiListingMapperImpl implements AiListingMapper {

    @Override
    public AiListingVerificationRequest toVerificationRequest(Listing listing) {
        if (listing == null) {
            log.warn("Cannot convert null listing to verification request");
            return null;
        }

        log.debug("Converting listing {} to AI verification request", listing.getListingId());

        return AiListingVerificationRequest.builder()
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .priceUnit(listing.getPriceUnit() != null ? listing.getPriceUnit().name() : null)
                .listingType(listing.getListingType() != null ? listing.getListingType().name() : null)
                .address(getAddressString(listing))
                .propertyType(convertProductType(listing.getProductType()))
                .area(listing.getArea() != null ? listing.getArea().doubleValue() : null)
                .amenities(convertAmenities(listing.getAmenities()))
                .images(buildImages(listing.getMedia()))
                .videos(buildVideos(listing.getMedia()))
                .metadata(buildMetadata(listing))
                .direction(listing.getDirection() != null ? listing.getDirection().name() : null)
                .furnishing(listing.getFurnishing() != null ? listing.getFurnishing().name() : null)
                .roomCapacity(listing.getRoomCapacity())
                .waterPrice(listing.getWaterPrice())
                .electricityPrice(listing.getElectricityPrice())
                .internetPrice(listing.getInternetPrice())
                .serviceFee(listing.getServiceFee())
                .build();
    }

    private String getAddressString(Listing listing) {
        if (listing.getAddress() == null) {
            return "Address not available";
        }

        // Use the new address format if available, otherwise fallback to old format
        String address = listing.getAddress().getFullNewAddress();
        if (!isValidAddress(address)) {
            address = listing.getAddress().getFullAddress();
        }

        // Final safety check: if still invalid (e.g., "N/A" from old data), use fallback
        return isValidAddress(address) ? address : "Address not available";
    }

    /**
     * Validates that an address string is meaningful (not null, blank, or too short).
     * AI Service requires address to have at least 5 characters.
     */
    private boolean isValidAddress(String address) {
        return address != null && address.trim().length() >= 5;
    }

    private HousingPropertyType convertProductType(Listing.ProductType productType) {
        if (productType == null) {
            return null;
        }

        try {
            // Map Listing.ProductType to HousingPropertyType
            return switch (productType) {
                case APARTMENT -> HousingPropertyType.APARTMENT;
                case HOUSE -> HousingPropertyType.HOUSE;
                case ROOM -> HousingPropertyType.ROOM;
                case STUDIO -> HousingPropertyType.STUDIO;
                case OFFICE -> HousingPropertyType.OFFICE;
                case STORE -> HousingPropertyType.STORE;
                default -> {
                    log.warn("Unknown product type: {}, defaulting to APARTMENT", productType);
                    yield HousingPropertyType.APARTMENT;
                }
            };
        } catch (Exception e) {
            log.warn("Error converting product type {}: {}", productType, e.getMessage());
            return HousingPropertyType.APARTMENT;
        }
    }

    private List<String> convertAmenities(List<Amenity> amenities) {
        if (amenities == null) {
            return null;
        }

        return amenities.stream()
                .map(Amenity::getName)
                .collect(Collectors.toList());
    }

    private AiListingVerificationRequest.PropertyMetadata buildMetadata(Listing listing) {
        return AiListingVerificationRequest.PropertyMetadata.builder()
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .build();
    }

    /**
     * Orders media exactly as the listing gallery shows it: primary first, then by
     * sortOrder. The AI numbers its per-media findings ("Ảnh N" / "Video N") by the
     * order it receives the media, so this MUST match the display order in
     * {@link ListingMapperImpl}. Otherwise "Ảnh 1" in the AI result points at a
     * different image than the admin sees first in the review dialog — the media
     * collection comes back in JPA/PK order, which is unrelated to display order.
     */
    private static final Comparator<Media> DISPLAY_ORDER =
            Comparator.comparing((Media m) -> !Boolean.TRUE.equals(m.getIsPrimary()))
                    .thenComparing(Media::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private List<String> buildImages(List<Media> mediaList) {
        return mediaList.stream()
                // ACTIVE-only + display order so the AI sees the same images, in the
                // same order, the admin/user does (mirrors ListingMapperImpl).
                .filter(media -> media.getStatus() == Media.MediaStatus.ACTIVE)
                .filter(media -> media.getMediaType() == Media.MediaType.IMAGE)
                .sorted(DISPLAY_ORDER)
                .map(Media::getUrl)
                .collect(Collectors.toList());
    }

    private List<AiListingVerificationRequest.VideoObject> buildVideos(List<Media> mediaList) {
        return mediaList.stream()
                .filter(media -> media.getStatus() == Media.MediaStatus.ACTIVE)
                .filter(media -> media.getMediaType() == Media.MediaType.VIDEO)
                .sorted(DISPLAY_ORDER)
                .map(media -> AiListingVerificationRequest.VideoObject.builder()
                        .url(media.getUrl())
                        .caption(media.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}