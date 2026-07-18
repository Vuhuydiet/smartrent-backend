package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.SavedListingResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.SavedListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import com.smartrent.mapper.SavedListingMapper;
import com.smartrent.service.listing.SavedListingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedListingServiceImpl implements SavedListingService {

    // Keep in sync with MAX_SAVED_LISTINGS in the FE (src/api/types/saved-listing.type.ts).
    private static final int MAX_SAVED_LISTINGS = 50;

    SavedListingRepository savedListingRepository;
    SavedListingMapper savedListingMapper;
    CacheManager cacheManager;

    private void evictPersonalizedCache(String userId) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(com.smartrent.config.Constants.CacheNames.LISTING_RECOMMENDATION_PERSONALIZED);
            if (cache != null) {
                cache.evict("user:" + userId + ":topN:8");
                cache.evict("user:" + userId + ":topN:9");
                cache.evict("user:" + userId + ":topN:10");
                cache.evict("user:" + userId + ":topN:11");
                cache.evict("user:" + userId + ":topN:12");
                cache.evict("user:" + userId + ":topN:15");
                cache.evict("user:" + userId + ":topN:20");
                log.info("Successfully evicted recommendation cache for user: {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict recommendation cache for user " + userId, e);
        }
    }

    @Override
    @Transactional
    public SavedListingResponse saveListing(SavedListingRequest request) {
        String userId = getCurrentUserId();
        log.info("Saving listing {} for user {}", request.getListingId(), userId);
        
        // Check if already saved
        if (savedListingRepository.existsByIdUserIdAndIdListingId(userId, request.getListingId())) {
            throw new RuntimeException("Listing is already saved by this user");
        }

        // Soft cap so a user's saved list can't grow unbounded. Reads the count
        // inside this method's transaction rather than relying on a caller-side
        // check, but a burst of concurrent saves from the same user could still
        // both read a count under the limit before either insert commits — a
        // narrow race that's acceptable for a favorites-style limit like this.
        long currentCount = savedListingRepository.countByIdUserId(userId);
        if (currentCount >= MAX_SAVED_LISTINGS) {
            throw new DomainException(DomainCode.SAVED_LISTING_LIMIT_EXCEEDED, MAX_SAVED_LISTINGS);
        }

        SavedListing savedListing = savedListingMapper.toEntity(request, userId);
        SavedListing saved = savedListingRepository.save(savedListing);
        
        log.info("Successfully saved listing {} for user {}", request.getListingId(), userId);
        evictPersonalizedCache(userId);
        return savedListingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void unsaveListing(Long listingId) {
        String userId = getCurrentUserId();
        log.info("Unsaving listing {} for user {}", listingId, userId);
        
        SavedListingId id = new SavedListingId(userId, listingId);
        if (!savedListingRepository.existsById(id)) {
            throw new RuntimeException("Saved listing not found");
        }
        
        savedListingRepository.deleteByIdUserIdAndIdListingId(userId, listingId);
        log.info("Successfully unsaved listing {} for user {}", listingId, userId);
        evictPersonalizedCache(userId);
    }

    @Override
    @Transactional
    public List<SavedListingResponse> getMySavedListings() {
        String userId = getCurrentUserId();
        log.info("Getting saved listings for user {}", userId);

        List<SavedListing> visibleSavedListings = fetchVisibleSavedListings(userId);

        return visibleSavedListings.stream()
                .map(savedListingMapper::toResponseWithListing)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PageResponse<SavedListingResponse> getMySavedListings(int page, int size) {
        String userId = getCurrentUserId();
        log.info("Getting saved listings for user {} with pagination - page: {}, size: {}", userId, page, size);

        // The saved list is capped at MAX_SAVED_LISTINGS, so fetching every row for
        // the user and paginating/filtering in memory is cheap and lets us reuse
        // Listing.isPubliclyVisible() as the single source of truth instead of
        // re-deriving its status rules in a native query.
        List<SavedListing> visibleSavedListings = fetchVisibleSavedListings(userId);

        int totalElements = visibleSavedListings.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int fromIndex = size > 0 ? Math.min((Math.max(page, 1) - 1) * size, totalElements) : 0;
        int toIndex = size > 0 ? Math.min(fromIndex + size, totalElements) : 0;

        List<SavedListingResponse> savedListingResponses = visibleSavedListings.subList(fromIndex, toIndex).stream()
                .map(savedListingMapper::toResponseWithListing)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} saved listings", savedListingResponses.size());

        return PageResponse.<SavedListingResponse>builder()
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .totalElements((long) totalElements)
                .data(savedListingResponses)
                .build();
    }

    /**
     * Fetches every saved-listing row for the user and drops (and deletes) any
     * whose listing is no longer publicly visible (expired, unpublished, taken
     * down by moderation, ...). {@link SavedListingRepository} has no visibility
     * filter of its own, so without this a stale favorite would keep showing
     * full listing data while opening it 404s.
     */
    private List<SavedListing> fetchVisibleSavedListings(String userId) {
        List<SavedListing> savedListings = savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<SavedListing> visible = new ArrayList<>();
        boolean removedAny = false;
        for (SavedListing savedListing : savedListings) {
            Listing listing = savedListing.getListing();
            if (listing != null && listing.isPubliclyVisible()) {
                visible.add(savedListing);
                continue;
            }

            Long listingId = savedListing.getId().getListingId();
            savedListingRepository.deleteByIdUserIdAndIdListingId(userId, listingId);
            removedAny = true;
            log.info("Removed stale saved listing {} for user {} (no longer publicly visible)", listingId, userId);
        }

        if (removedAny) {
            evictPersonalizedCache(userId);
        }

        return visible;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isListingSaved(Long listingId) {
        String userId = getCurrentUserId();
        return savedListingRepository.existsByIdUserIdAndIdListingId(userId, listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMySavedListingsCount() {
        String userId = getCurrentUserId();
        return savedListingRepository.countByIdUserId(userId);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
