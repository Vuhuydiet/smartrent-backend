package com.smartrent.service.viptier.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.VipTierDetailResponse;
import com.smartrent.dto.response.VipTierMediaLimitResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.VipTierDetailRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Media;
import com.smartrent.infra.repository.entity.VipTierDetail;
import com.smartrent.service.viptier.VipTierDetailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VipTierDetailServiceImpl implements VipTierDetailService {

    VipTierDetailRepository vipTierDetailRepository;
    ListingRepository listingRepository;
    MediaRepository mediaRepository;
    ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VipTierDetailResponse> getAllActiveTiers() {
        log.info("Getting all active VIP tiers");
        return vipTierDetailRepository.findByIsActiveTrueOrderByTierLevelAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VipTierDetailResponse> getAllTiers() {
        log.info("Getting all VIP tiers");
        return vipTierDetailRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VipTierDetailResponse getTierByCode(String tierCode) {
        log.info("Getting VIP tier by code: {}", tierCode);
        VipTierDetail tier = vipTierDetailRepository.findByTierCode(tierCode)
                .orElseThrow(() -> new RuntimeException("VIP tier not found: " + tierCode));
        return mapToResponse(tier);
    }

    @Override
    @Transactional(readOnly = true)
    public VipTierDetailResponse getTierById(Long tierId) {
        log.info("Getting VIP tier by ID: {}", tierId);
        VipTierDetail tier = vipTierDetailRepository.findById(tierId)
                .orElseThrow(() -> new RuntimeException("VIP tier not found: " + tierId));
        return mapToResponse(tier);
    }

    @Override
    @Transactional(readOnly = true)
    public VipTierMediaLimitResponse getMediaLimitsByTierCode(String tierCode) {
        VipTierDetail tier = loadTier(tierCode);
        return VipTierMediaLimitResponse.builder()
                .tierCode(tier.getTierCode())
                .maxImages(tier.getMaxImages())
                .maxVideos(tier.getMaxVideos())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VipTierMediaLimitResponse getMediaLimitsForListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND));

        String tierCode = listing.getVipType() != null ? listing.getVipType().name() : "NORMAL";
        VipTierDetail tier = loadTier(tierCode);

        int currentImages = (int) mediaRepository
                .findByListing_ListingIdAndMediaTypeAndStatusOrderBySortOrderAsc(
                        listingId, Media.MediaType.IMAGE, Media.MediaStatus.ACTIVE)
                .size();
        int currentVideos = (int) mediaRepository
                .findByListing_ListingIdAndMediaTypeAndStatusOrderBySortOrderAsc(
                        listingId, Media.MediaType.VIDEO, Media.MediaStatus.ACTIVE)
                .size();

        return VipTierMediaLimitResponse.builder()
                .tierCode(tier.getTierCode())
                .maxImages(tier.getMaxImages())
                .maxVideos(tier.getMaxVideos())
                .currentImages(currentImages)
                .currentVideos(currentVideos)
                .remainingImages(Math.max(0, tier.getMaxImages() - currentImages))
                .remainingVideos(Math.max(0, tier.getMaxVideos() - currentVideos))
                .build();
    }

    private VipTierDetail loadTier(String tierCode) {
        if (tierCode == null || tierCode.isBlank()) {
            throw new AppException(DomainCode.VIP_TIER_NOT_CONFIGURED,
                    String.format(DomainCode.VIP_TIER_NOT_CONFIGURED.getMessage(), "<null>"));
        }
        String normalized = tierCode.toUpperCase();
        return vipTierDetailRepository.findByTierCode(normalized)
                .orElseThrow(() -> new AppException(DomainCode.VIP_TIER_NOT_CONFIGURED,
                        String.format(DomainCode.VIP_TIER_NOT_CONFIGURED.getMessage(), normalized)));
    }

    private VipTierDetailResponse mapToResponse(VipTierDetail tier) {
        List<String> featuresList = null;
        if (tier.getFeatures() != null) {
            try {
                featuresList = objectMapper.readValue(tier.getFeatures(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse features JSON for tier {}: {}", tier.getTierCode(), e.getMessage());
            }
        }

        return VipTierDetailResponse.builder()
                .tierId(tier.getTierId())
                .tierCode(tier.getTierCode())
                .tierName(tier.getTierName())
                .tierNameEn(tier.getTierNameEn())
                .tierLevel(tier.getTierLevel())
                .pricePerDay(tier.getPricePerDay())
                .price10Days(tier.getPrice10Days())
                .price15Days(tier.getPrice15Days())
                .price30Days(tier.getPrice30Days())
                .maxImages(tier.getMaxImages())
                .maxVideos(tier.getMaxVideos())
                .hasBadge(tier.getHasBadge())
                .badgeName(tier.getBadgeName())
                .badgeColor(tier.getBadgeColor())
                .autoApprove(tier.getAutoApprove())
                .noAds(tier.getNoAds())
                .priorityDisplay(tier.getPriorityDisplay())
                .hasShadowListing(tier.getHasShadowListing())
                .description(tier.getDescription())
                .features(featuresList)
                .isActive(tier.getIsActive())
                .displayOrder(tier.getDisplayOrder())
                .build();
    }
}

