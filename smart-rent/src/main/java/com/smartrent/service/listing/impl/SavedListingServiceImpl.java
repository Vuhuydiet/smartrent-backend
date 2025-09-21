package com.smartrent.service.listing.impl;

import com.smartrent.controller.dto.request.SavedListingRequest;
import com.smartrent.controller.dto.response.SavedListingResponse;
import com.smartrent.infra.repository.SavedListingRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedListingServiceImpl implements SavedListingService {

    SavedListingRepository savedListingRepository;
    SavedListingMapper savedListingMapper;

    @Override
    @Transactional
    public SavedListingResponse saveListing(SavedListingRequest request) {
        String userId = getCurrentUserId();
        log.info("Saving listing {} for user {}", request.getListingId(), userId);
        
        // Check if already saved
        if (savedListingRepository.existsByIdUserIdAndIdListingId(userId, request.getListingId())) {
            throw new RuntimeException("Listing is already saved by this user");
        }
        
        SavedListing savedListing = savedListingMapper.toEntity(request, userId);
        SavedListing saved = savedListingRepository.save(savedListing);
        
        log.info("Successfully saved listing {} for user {}", request.getListingId(), userId);
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedListingResponse> getMySavedListings() {
        String userId = getCurrentUserId();
        log.info("Getting saved listings for user {}", userId);
        
        List<SavedListing> savedListings = savedListingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return savedListings.stream()
                .map(savedListingMapper::toResponse)
                .collect(Collectors.toList());
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
