package com.smartrent.service.phoneclickdetail.impl;

import com.smartrent.dto.request.PhoneClickRequest;
import com.smartrent.dto.response.PhoneClickResponse;
import com.smartrent.dto.response.PhoneClickStatsResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PhoneClickDetailRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.PhoneClickDetail;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.phoneclickdetail.PhoneClickDetailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PhoneClickDetailServiceImpl implements PhoneClickDetailService {

    PhoneClickDetailRepository phoneClickDetailRepository;
    ListingRepository listingRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public PhoneClickResponse trackPhoneClick(PhoneClickRequest request, String userId, String ipAddress, String userAgent) {
        log.info("Tracking phone click for listing {} by user {}", request.getListingId(), userId);

        // Verify listing exists
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + request.getListingId()));

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if user already clicked on this listing
        boolean alreadyClicked = phoneClickDetailRepository.existsByListing_ListingIdAndUser_UserId(
                request.getListingId(), userId);

        if (alreadyClicked) {
            log.info("User {} already clicked on listing {}, updating timestamp", userId, request.getListingId());
            // User already clicked, we can either:
            // 1. Do nothing and return existing record
            // 2. Create a new record to track multiple clicks
            // For now, we'll create a new record to track all interactions
        }

        // Create phone click record
        PhoneClickDetail phoneClickDetail = PhoneClickDetail.builder()
                .listing(listing)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        PhoneClickDetail saved = phoneClickDetailRepository.save(phoneClickDetail);
        log.info("Phone click tracked successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhoneClickResponse> getPhoneClicksByListing(Long listingId) {
        log.info("Getting phone clicks for listing {}", listingId);

        // Verify listing exists
        listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        List<PhoneClickDetail> phoneClickDetails = phoneClickDetailRepository.findDistinctUsersByListingId(listingId);
        
        return phoneClickDetails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhoneClickResponse> getPhoneClicksByUser(String userId) {
        log.info("Getting phone clicks by user {}", userId);

        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        List<PhoneClickDetail> phoneClickDetails = phoneClickDetailRepository.findByUser_UserIdOrderByClickedAtDesc(userId);
        
        return phoneClickDetails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PhoneClickStatsResponse getPhoneClickStats(Long listingId) {
        log.info("Getting phone click stats for listing {}", listingId);

        // Verify listing exists
        listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        long totalClicks = phoneClickDetailRepository.countByListing_ListingId(listingId);
        long uniqueUsers = phoneClickDetailRepository.countDistinctUsersByListingId(listingId);

        return PhoneClickStatsResponse.builder()
                .listingId(listingId)
                .totalClicks(totalClicks)
                .uniqueUsers(uniqueUsers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhoneClickResponse> getPhoneClicksForOwnerListings(String ownerId) {
        log.info("Getting phone clicks for all listings owned by user {}", ownerId);

        // Verify user exists
        userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + ownerId));

        List<PhoneClickDetail> phoneClickDetails = phoneClickDetailRepository.findByListingOwnerIdOrderByClickedAtDesc(ownerId);
        
        return phoneClickDetails.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map PhoneClickDetail entity to PhoneClickResponse DTO
     */
    private PhoneClickResponse mapToResponse(PhoneClickDetail phoneClickDetail) {
        User user = phoneClickDetail.getUser();
        
        return PhoneClickResponse.builder()
                .id(phoneClickDetail.getId())
                .listingId(phoneClickDetail.getListing().getListingId())
                .userId(user.getUserId())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .userEmail(user.getEmail())
                .userContactPhone(user.getContactPhoneNumber())
                .userContactPhoneVerified(user.getContactPhoneVerified())
                .clickedAt(phoneClickDetail.getClickedAt())
                .ipAddress(phoneClickDetail.getIpAddress())
                .build();
    }
}

