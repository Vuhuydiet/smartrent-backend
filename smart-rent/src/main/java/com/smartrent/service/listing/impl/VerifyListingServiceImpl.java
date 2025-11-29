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
        listing.setIsVerify(request.getVerified());

        // Save the updated listing
        Listing updatedListing = listingRepository.save(listing);

        log.info("Successfully updated listing {} verification status to: {}",
                listingId, request.getVerified());

        // Build user and address responses
        com.smartrent.dto.response.UserCreationResponse user = userRepository.findById(updatedListing.getUserId())
                .map(userMapper::mapFromUserEntityToUserCreationResponse)
                .orElse(null);
        com.smartrent.dto.response.AddressResponse addressResponse =
                updatedListing.getAddress() != null ? addressMapper.toResponse(updatedListing.getAddress()) : null;

        return listingMapper.toResponse(updatedListing, user, addressResponse);
    }
}
