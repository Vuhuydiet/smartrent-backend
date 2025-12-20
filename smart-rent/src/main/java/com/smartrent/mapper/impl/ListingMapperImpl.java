package com.smartrent.mapper.impl;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.AdminVerificationInfo;
import com.smartrent.dto.response.AmenityResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponseForOwner;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.MediaResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.AmenityMapper;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.MediaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListingMapperImpl implements ListingMapper {

    private final AmenityMapper amenityMapper;
    private final MediaMapper mediaMapper;
    @Override
    public Listing toEntity(ListingCreationRequest req) {
        Listing.VipType vipType = req.getVipType() != null ? Listing.VipType.valueOf(req.getVipType()) : null;
        return Listing.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .userId(req.getUserId())
                .postDate(req.getPostDate() != null ? req.getPostDate() : java.time.LocalDateTime.now())
                .expiryDate(req.getExpiryDate())
                .listingType(req.getListingType() != null ? Listing.ListingType.valueOf(req.getListingType()) : null)
                .verified(req.getVerified() != null ? req.getVerified() : false)
                .isVerify(req.getIsVerify() != null ? req.getIsVerify() : true)
                .expired(req.getExpired() != null ? req.getExpired() : false)
                .vipType(vipType)
                .vipTypeSortOrder(Listing.getVipTypeSortOrder(vipType))
                .categoryId(req.getCategoryId())
                .productType(req.getProductType() != null ? Listing.ProductType.valueOf(req.getProductType()) : null)
                .price(req.getPrice())
                .priceUnit(req.getPriceUnit() != null ? Listing.PriceUnit.valueOf(req.getPriceUnit()) : null)
                // Note: address is NOT set here - it will be set in the service layer after creation
                // to ensure transactional integrity between Address and Listing creation
                .area(req.getArea())
                .bedrooms(req.getBedrooms())
                .bathrooms(req.getBathrooms())
                .direction(req.getDirection() != null ? Listing.Direction.valueOf(req.getDirection()) : null)
                .furnishing(req.getFurnishing() != null ? Listing.Furnishing.valueOf(req.getFurnishing()) : null)
                .roomCapacity(req.getRoomCapacity())
                .waterPrice(req.getWaterPrice())
                .electricityPrice(req.getElectricityPrice())
                .internetPrice(req.getInternetPrice())
                .serviceFee(req.getServiceFee())
                .durationDays(req.getDurationDays() != null ? req.getDurationDays() : 30)
                .useMembershipQuota(req.getUseMembershipQuota() != null ? req.getUseMembershipQuota() : false)
                .paymentProvider(req.getPaymentProvider())
                .build();
    }

    @Override
    public ListingResponse toResponse(Listing entity, UserCreationResponse user, AddressResponse address) {
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
        // Map media to MediaResponse list
        List<MediaResponse> mediaResponses = null;
        if (entity.getMedia() != null && !entity.getMedia().isEmpty()) {
            mediaResponses = entity.getMedia().stream()
                    .filter(media -> media.getStatus() == Media.MediaStatus.ACTIVE)
                    .sorted((m1, m2) -> {
                        // Primary media first
                        if (m1.getIsPrimary() && !m2.getIsPrimary()) return -1;
                        if (!m1.getIsPrimary() && m2.getIsPrimary()) return 1;
                        // Then sort by sortOrder
                        return m1.getSortOrder().compareTo(m2.getSortOrder());
                    })
                    .map(mediaMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return ListingResponse.builder()
                .listingId(entity.getListingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .user(user)
                .postDate(entity.getPostDate())
                .expiryDate(entity.getExpiryDate())
                .listingType(entity.getListingType() != null ? entity.getListingType().name() : null)
                .verified(entity.getVerified())
                .isVerify(entity.getIsVerify())
                .isDraft(entity.getIsDraft())
                .listingStatus(entity.computeListingStatus().name())
                .expired(entity.getExpired())
                .vipType(entity.getVipType() != null ? entity.getVipType().name() : null)
                .categoryId(entity.getCategoryId())
                .productType(entity.getProductType() != null ? entity.getProductType().name() : null)
                .price(entity.getPrice())
                .priceUnit(entity.getPriceUnit() != null ? entity.getPriceUnit().name() : null)
                .address(address)
                .area(entity.getArea())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .direction(entity.getDirection() != null ? entity.getDirection().name() : null)
                .furnishing(entity.getFurnishing() != null ? entity.getFurnishing().name() : null)
                .roomCapacity(entity.getRoomCapacity())
                .waterPrice(entity.getWaterPrice())
                .electricityPrice(entity.getElectricityPrice())
                .internetPrice(entity.getInternetPrice())
                .serviceFee(entity.getServiceFee())
                .amenities(amenityResponses)
                .media(mediaResponses)
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

        // Map media to MediaResponse list
        List<MediaResponse> mediaResponses = null;
        if (entity.getMedia() != null && !entity.getMedia().isEmpty()) {
            mediaResponses = entity.getMedia().stream()
                    .filter(media -> media.getStatus() == Media.MediaStatus.ACTIVE)
                    .sorted((m1, m2) -> {
                        // Primary media first
                        if (m1.getIsPrimary() && !m2.getIsPrimary()) return -1;
                        if (!m1.getIsPrimary() && m2.getIsPrimary()) return 1;
                        // Then sort by sortOrder
                        return m1.getSortOrder().compareTo(m2.getSortOrder());
                    })
                    .map(mediaMapper::toResponse)
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
                .addressId(entity.getAddress() != null ? entity.getAddress().getAddressId() : null)
                .area(entity.getArea())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .direction(entity.getDirection() != null ? entity.getDirection().name() : null)
                .furnishing(entity.getFurnishing() != null ? entity.getFurnishing().name() : null)
                .roomCapacity(entity.getRoomCapacity())
                .waterPrice(entity.getWaterPrice())
                .electricityPrice(entity.getElectricityPrice())
                .internetPrice(entity.getInternetPrice())
                .serviceFee(entity.getServiceFee())
                .amenities(amenityResponses)
                .media(mediaResponses)
                .adminVerification(adminVerificationInfo)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    @Override
    public ListingResponseForOwner toResponseForOwner(
            Listing entity,
            UserCreationResponse user,
            List<MediaResponse> media,
            AddressResponse address,
            ListingResponseForOwner.PaymentInfo paymentInfo,
            ListingResponseForOwner.ListingStatistics statistics,
            String verificationNotes,
            String rejectionReason) {

        // Map amenities to AmenityResponse list
        List<AmenityResponse> amenityResponses = null;
        if (entity.getAmenities() != null && !entity.getAmenities().isEmpty()) {
            amenityResponses = entity.getAmenities().stream()
                    .map(amenityMapper::toResponse)
                    .collect(Collectors.toList());
        }


        return ListingResponseForOwner.builder()
                // Base fields from ListingResponse
                .listingId(entity.getListingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .user(user)
                .postDate(entity.getPostDate())
                .expiryDate(entity.getExpiryDate())
                .listingType(entity.getListingType() != null ? entity.getListingType().name() : null)
                .verified(entity.getVerified())
                .isVerify(entity.getIsVerify())
                .expired(entity.getExpired())
                .isDraft(entity.getIsDraft())
                .vipType(entity.getVipType() != null ? entity.getVipType().name() : null)
                .categoryId(entity.getCategoryId())
                .productType(entity.getProductType() != null ? entity.getProductType().name() : null)
                .price(entity.getPrice())
                .priceUnit(entity.getPriceUnit() != null ? entity.getPriceUnit().name() : null)
                .address(address)
                .area(entity.getArea())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .direction(entity.getDirection() != null ? entity.getDirection().name() : null)
                .furnishing(entity.getFurnishing() != null ? entity.getFurnishing().name() : null)
                .roomCapacity(entity.getRoomCapacity())
                .waterPrice(entity.getWaterPrice())
                .electricityPrice(entity.getElectricityPrice())
                .internetPrice(entity.getInternetPrice())
                .serviceFee(entity.getServiceFee())
                .amenities(amenityResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())

                // Owner-specific fields
                .postSource(entity.getPostSource() != null ? entity.getPostSource().name() : null)
                .transactionId(entity.getTransactionId())
                .isShadow(entity.getIsShadow())
                .parentListingId(entity.getParentListingId())
                .durationDays(entity.getDurationDays())
                .useMembershipQuota(entity.getUseMembershipQuota())
                .paymentProvider(entity.getPaymentProvider())
                .media(media)
                .address(address)
                .paymentInfo(paymentInfo)
                .statistics(statistics)
                .verificationNotes(verificationNotes)
                .rejectionReason(rejectionReason)
                .build();
    }
}
