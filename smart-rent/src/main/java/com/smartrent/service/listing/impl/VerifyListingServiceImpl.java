package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.ListingStatusChangeRequest;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.service.listing.VerifyListingService;
import com.smartrent.service.moderation.ListingModerationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    AdminRepository adminRepository;
    ListingModerationService listingModerationService;

    @Override
    @Transactional
    public ListingResponseWithAdmin changeListingStatus(Long listingId, ListingStatusChangeRequest request) {
        // Get admin from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        // ── New moderation path (decision-based) ──
        if (request.isNewFormat()) {
            log.info("Admin {} moderating listing {} with decision: {}", adminId, listingId, request.getDecision());
            return listingModerationService.moderateListing(listingId, request, adminId);
        }

        // ── Legacy path (verified boolean) ──
        log.info("Changing listing status for listing ID: {} to verified: {}", listingId, request.getVerified());

        // Delegate to moderation service even for legacy requests (it handles the adapter)
        return listingModerationService.moderateListing(listingId, request, adminId);
    }
}
