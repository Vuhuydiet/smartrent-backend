package com.smartrent.mapper.impl;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.AdminVerificationInfo;
import com.smartrent.dto.response.AmenityResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.AmenityMapper;
import com.smartrent.mapper.ListingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListingMapperImpl implements ListingMapper {

    private final AmenityMapper amenityMapper;
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
                .categoryId(req.getCategoryId())
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
        // Map amenities to AmenityResponse list
        List<AmenityResponse> amenityResponses = null;
        Set<Long> amenityIds = null;

        if (entity.getAmenities() != null && !entity.getAmenities().isEmpty()) {
            amenityResponses = entity.getAmenities().stream()
                    .map(amenityMapper::toResponse)
                    .collect(Collectors.toList());

            // Also populate amenityIds for backward compatibility
            amenityIds = entity.getAmenities().stream()
                    .map(Amenity::getAmenityId)
                    .collect(Collectors.toSet());
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

    @Override
    public ListingResponseWithAdmin toResponseWithAdmin(Listing entity, Admin verifyingAdmin, String verificationStatus, String verificationNotes) {
        // Map amenities to AmenityResponse list
        List<AmenityResponse> amenityResponses = null;

        if (entity.getAmenities() != null && !entity.getAmenities().isEmpty()) {
            amenityResponses = entity.getAmenities().stream()
                    .map(amenityMapper::toResponse)
                    .collect(Collectors.toList());
        }

        // Build admin verification info if admin is provided
        AdminVerificationInfo adminVerificationInfo = null;
        if (verifyingAdmin != null) {
            String adminName = (verifyingAdmin.getFirstName() != null ? verifyingAdmin.getFirstName() : "") +
                             " " +
                             (verifyingAdmin.getLastName() != null ? verifyingAdmin.getLastName() : "");
            adminName = adminName.trim();

            adminVerificationInfo = AdminVerificationInfo.builder()
                    .adminId(verifyingAdmin.getAdminId())
                    .adminName(adminName.isEmpty() ? null : adminName)
                    .adminEmail(verifyingAdmin.getEmail())
                    .verifiedAt(entity.getUpdatedAt())
                    .verificationStatus(verificationStatus != null ? verificationStatus : (entity.getVerified() ? "APPROVED" : (entity.getIsVerify() ? "PENDING" : "PENDING")))
                    .verificationNotes(verificationNotes)
                    .build();
        }

        return ListingResponseWithAdmin.builder()
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
                .adminVerification(adminVerificationInfo)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
