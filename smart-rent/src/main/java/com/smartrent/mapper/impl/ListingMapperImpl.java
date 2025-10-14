package com.smartrent.mapper.impl;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.AmenityResponse;
import com.smartrent.dto.response.ImageResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.dto.response.VideoResponse;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.AmenityMapper;
import com.smartrent.mapper.ImageMapper;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.PricingHistoryMapper;
import com.smartrent.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListingMapperImpl implements ListingMapper {

    private final AmenityMapper amenityMapper;
    private final PricingHistoryMapper pricingHistoryMapper;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    @Override
    public Listing toEntity(ListingCreationRequest req) {
        return Listing.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .userId(req.getUserId())
                .expiryDate(req.getExpiryDate())
                .listingType(req.getListingType() != null ? Listing.ListingType.valueOf(req.getListingType()) : null)
                .verified(req.getVerified())
                .isVerify(req.getIsVerify())
                .expired(req.getExpired())
                .vipType(req.getVipType() != null ? Listing.VipType.valueOf(req.getVipType()) : null)
                .status(Listing.ListingStatus.PENDING) // Always set to PENDING for new listings\n                .categoryId(req.getCategoryId())
                .productType(req.getProductType() != null ? Listing.ProductType.valueOf(req.getProductType()) : null)
                .price(req.getPrice())
                .priceUnit(req.getPriceUnit() != null ? Listing.PriceUnit.valueOf(req.getPriceUnit()) : null)
                .addressId(req.getAddressId())
                .area(req.getArea())
                .bedrooms(req.getBedrooms())
                .bathrooms(req.getBathrooms())
                .direction(req.getDirection() != null ? Listing.Direction.valueOf(req.getDirection()) : null)
                .furnishing(req.getFurnishing() != null ? Listing.Furnishing.valueOf(req.getFurnishing()) : null)
                .propertyType(req.getPropertyType() != null ? Listing.PropertyType.valueOf(req.getPropertyType()) : null)
                .roomCapacity(req.getRoomCapacity())
                .build();
    }

    @Override
    public ListingResponse toResponse(Listing entity) {
        List<AmenityResponse> amenityResponses = Collections.emptyList();
        if (entity.getAmenities() != null && !entity.getAmenities().isEmpty()) {
            amenityResponses = entity.getAmenities().stream()
                    .map(amenityMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Map images
        List<ImageResponse> imageResponses = Collections.emptyList();
        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            imageResponses = entity.getImages().stream()
                    .map(imageMapper::toResponse)
                    .sorted((a, b) -> {
                        // Primary images first, then by sortOrder
                        if (a.getIsPrimary() && !b.getIsPrimary()) return -1;
                        if (!a.getIsPrimary() && b.getIsPrimary()) return 1;
                        return Integer.compare(a.getSortOrder(), b.getSortOrder());
                    })
                    .collect(Collectors.toList());
        }

        // Map videos
        List<VideoResponse> videoResponses = Collections.emptyList();
        if (entity.getVideos() != null && !entity.getVideos().isEmpty()) {
            videoResponses = entity.getVideos().stream()
                    .map(videoMapper::toResponse)
                    .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                    .collect(Collectors.toList());
        }

        // Map pricing history
        PricingHistoryResponse currentPricing = null;
        List<PricingHistoryResponse> priceHistory = Collections.emptyList();

        if (entity.getPricingHistories() != null && !entity.getPricingHistories().isEmpty()) {
            priceHistory = entity.getPricingHistories().stream()
                    .map(pricingHistoryMapper::toResponse)
                    .collect(Collectors.toList());

            // Find current pricing
            currentPricing = priceHistory.stream()
                    .filter(PricingHistoryResponse::getIsCurrent)
                    .findFirst()
                    .orElse(null);
        }

        return ListingResponse.builder()
                .listingId(entity.getListingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .userId(entity.getUserId())
                .postDate(entity.getPostDate())
                .expiryDate(entity.getExpiryDate())
                .listingType(entity.getListingType() != null ? entity.getListingType().name() : null)
                .verified(entity.getVerified())
                .isVerify(entity.getIsVerify())
                .expired(entity.getExpired())
                .vipType(entity.getVipType() != null ? entity.getVipType().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .categoryId(entity.getCategoryId())
                .productType(entity.getProductType() != null ? entity.getProductType().name() : null)
                .price(entity.getPrice())
                .priceUnit(entity.getPriceUnit() != null ? entity.getPriceUnit().name() : null)
                .addressId(entity.getAddressId())
                .area(entity.getArea())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .direction(entity.getDirection() != null ? entity.getDirection().name() : null)
                .furnishing(entity.getFurnishing() != null ? entity.getFurnishing().name() : null)
                .propertyType(entity.getPropertyType() != null ? entity.getPropertyType().name() : null)
                .roomCapacity(entity.getRoomCapacity())
                .amenities(amenityResponses)
                .images(imageResponses)
                .videos(videoResponses)
                .currentPricing(currentPricing)
                .priceHistory(priceHistory)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public ListingCreationResponse toCreationResponse(Listing entity) {
        return ListingCreationResponse.builder()
                .listingId(entity.getListingId())
                .status("CREATED")
                .build();
    }
}
