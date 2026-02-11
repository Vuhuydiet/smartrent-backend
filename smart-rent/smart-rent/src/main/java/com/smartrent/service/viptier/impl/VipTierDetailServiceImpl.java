package com.smartrent.service.viptier.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.VipTierDetailResponse;
import com.smartrent.infra.repository.VipTierDetailRepository;
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

