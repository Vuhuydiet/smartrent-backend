package com.smartrent.service.takedown.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.TakeDownListingRequest;
import com.smartrent.dto.response.TakeDownResponse;
import com.smartrent.enums.ListingStatus;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.takedown.TakeDownService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Mirrors PushServiceImpl. Where push promotes a DISPLAYING listing to the top
 * (post_date / pushed_at = now), take-down expires it: expired = true and
 * expiry_date = now, so computeListingStatus() returns EXPIRED and the listing
 * drops out of public search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TakeDownServiceImpl implements TakeDownService {

    ListingRepository listingRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_BROWSE, allEntries = true),
            @CacheEvict(cacheNames = Constants.CacheNames.LISTING_DETAIL, allEntries = true)
    })
    public TakeDownResponse takeDownListing(String userId, TakeDownListingRequest request) {
        log.info("Taking down listing {} for user {}", request.getListingId(), userId);

        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found: " + request.getListingId()));

        if (!listing.getUserId().equals(userId)) {
            throw new RuntimeException("Listing does not belong to user");
        }

        validateListingCanBeTakenDown(listing);

        LocalDateTime now = LocalDateTime.now();
        listing.setExpired(true);
        listing.setExpiryDate(now);
        listingRepository.save(listing);

        log.info("Successfully took down listing {}", request.getListingId());
        return TakeDownResponse.builder()
                .listingId(listing.getListingId())
                .userId(userId)
                .takenDownAt(now)
                .message("Listing taken down successfully")
                .build();
    }

    private void validateListingCanBeTakenDown(Listing listing) {
        ListingStatus status = listing.computeListingStatus();
        if (status != ListingStatus.DISPLAYING && status != ListingStatus.EXPIRING_SOON) {
            throw new RuntimeException(
                    "Only displaying listings can be taken down. Current status: " + status);
        }
    }
}
