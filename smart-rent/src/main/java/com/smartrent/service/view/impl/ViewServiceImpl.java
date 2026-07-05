package com.smartrent.service.view.impl;

import com.smartrent.dto.request.ViewTrackRequest;
import com.smartrent.dto.response.ViewTrackResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.ViewRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.View;
import com.smartrent.service.view.ViewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ViewServiceImpl implements ViewService {

    private static final int DEDUPE_WINDOW_MINUTES = 30;

    ListingRepository listingRepository;
    ViewRepository viewRepository;

    @Override
    @Transactional
    public ViewTrackResponse trackView(ViewTrackRequest request, String userId, String ipAddress, String userAgent) {
        Long listingId = request.getListingId();

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (ipAddress != null) {
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(DEDUPE_WINDOW_MINUTES);
            boolean alreadyViewed = viewRepository
                    .existsByIpAddressAndListing_ListingIdAndViewedAtAfter(ipAddress, listingId, windowStart);
            if (alreadyViewed) {
                log.debug("Duplicate view from IP {} on listing {} within {} minutes, ignoring",
                        ipAddress, listingId, DEDUPE_WINDOW_MINUTES);
                return ViewTrackResponse.builder()
                        .listingId(listingId)
                        .recorded(false)
                        .viewedAt(LocalDateTime.now())
                        .build();
            }
        }

        View view = View.builder()
                .listing(listing)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        View saved = viewRepository.save(view);
        log.debug("View tracked for listing {} with ID: {}", listingId, saved.getId());

        return ViewTrackResponse.builder()
                .listingId(listingId)
                .recorded(true)
                .viewedAt(saved.getViewedAt())
                .build();
    }
}
