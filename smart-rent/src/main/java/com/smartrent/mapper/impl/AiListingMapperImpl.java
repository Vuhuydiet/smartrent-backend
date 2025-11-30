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
                .address(getAddressString(listing))
                .propertyType(convertProductType(listing.getProductType()))
                .area(listing.getArea() != null ? listing.getArea().doubleValue() : null)
                .amenities(convertAmenities(listing.getAmenities()))
                .images(buildImages(listing.getMedia()))
                .videos(buildVideos(listing.getMedia()))
                .metadata(buildMetadata(listing))
                .build();
    }

    private String getAddressString(Listing listing) {
        if (listing.getAddress() == null) {
            return "Address not available";
        }
        
        // Use the new address format if available, otherwise fallback to old format
        String address = listing.getAddress().getFullNewAddress();
        if (address == null || address.trim().isEmpty()) {
            address = listing.getAddress().getFullAddress();
        }
        
        return address != null ? address : "Address not available";
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
                case OFFICE -> {
                    log.debug("Mapping OFFICE product type to APARTMENT for AI verification");
                    yield HousingPropertyType.APARTMENT;
                }
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

    private List<String> buildImages(List<Media> mediaList) {
        return mediaList.stream()
                .filter(media -> media.getMediaType() == Media.MediaType.IMAGE)
                .map(Media::getUrl)
                .collect(Collectors.toList());
    }

    private List<AiListingVerificationRequest.VideoObject> buildVideos(List<Media> mediaList) {
        return mediaList.stream()
                .filter(media -> media.getMediaType() == Media.MediaType.VIDEO)
                .map(media -> AiListingVerificationRequest.VideoObject.builder()
                        .url(media.getUrl())
                        .caption(media.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}