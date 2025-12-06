package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.listing.VerifyListingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerifyListingServiceImpl implements VerifyListingService {

    ListingRepository listingRepository;
    ListingMapper listingMapper;
    com.smartrent.mapper.UserMapper userMapper;
    com.smartrent.mapper.AddressMapper addressMapper;
    com.smartrent.infra.repository.UserRepository userRepository;

    @Override
    @Transactional
    public ListingResponse changeListingStatus(Long listingId, ListingStatusChangeRequest request) {
        log.info("Changing listing status for listing ID: {} to verified: {}", listingId, request.getVerified());

        // Find the listing
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new DomainException(DomainCode.LISTING_NOT_FOUND));

        // Update verification status
        listing.setVerified(request.getVerified());

        // If approving (verified=true), keep isVerify=true to indicate it has been reviewed
        // If rejecting (verified=false), set isVerify=false to mark as REJECTED status
        if (Boolean.TRUE.equals(request.getVerified())) {
            // Approved - keep isVerify=true (should already be true if it was in review)
            listing.setIsVerify(true);

            // Calculate expiryDate based on approval time and postDate
            // If approval time > postDate (user's chosen time), use approval time + durationDays
            // Otherwise, use postDate + durationDays
            java.time.LocalDateTime approvalTime = java.time.LocalDateTime.now();
            java.time.LocalDateTime postDate = listing.getPostDate();
            Integer durationDays = listing.getDurationDays() != null ? listing.getDurationDays() : 30;

            if (postDate != null && approvalTime.isAfter(postDate)) {
                // Approval time is after user's chosen postDate
                // Use approval time as base for expiry calculation
                listing.setExpiryDate(approvalTime.plusDays(durationDays));
                log.info("Listing {} approved after postDate. ExpiryDate calculated from approval time: {} + {} days = {}",
                        listingId, approvalTime, durationDays, listing.getExpiryDate());
            } else {
                // Use postDate as base for expiry calculation
                java.time.LocalDateTime baseDate = postDate != null ? postDate : approvalTime;
                listing.setExpiryDate(baseDate.plusDays(durationDays));
                log.info("Listing {} approved. ExpiryDate calculated from postDate: {} + {} days = {}",
                        listingId, baseDate, durationDays, listing.getExpiryDate());
            }
        } else {
            // Rejected - set isVerify=false to indicate rejection
            listing.setIsVerify(false);
        }

        // Save the updated listing
        Listing updatedListing = listingRepository.save(listing);

        log.info("Successfully updated listing {} verification status to: {}, isVerify: {}, reason: {}",
                listingId, request.getVerified(), listing.getIsVerify(), request.getReason());

        // Build user and address responses
        com.smartrent.dto.response.UserCreationResponse user = userRepository.findById(updatedListing.getUserId())
                .map(userMapper::mapFromUserEntityToUserCreationResponse)
                .orElse(null);
        com.smartrent.dto.response.AddressResponse addressResponse =
                updatedListing.getAddress() != null ? addressMapper.toResponse(updatedListing.getAddress()) : null;

        return listingMapper.toResponse(updatedListing, user, addressResponse);
    }
}
