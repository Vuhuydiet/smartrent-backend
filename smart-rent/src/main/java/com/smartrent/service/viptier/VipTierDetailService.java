package com.smartrent.service.viptier;

import com.smartrent.dto.response.VipTierDetailResponse;
import com.smartrent.dto.response.VipTierMediaLimitResponse;

import java.util.List;

/**
 * Service interface for managing VIP tier details
 */
public interface VipTierDetailService {

    /**
     * Get all active VIP tiers
     *
     * @return List of active VIP tier details
     */
    List<VipTierDetailResponse> getAllActiveTiers();

    /**
     * Get all VIP tiers (including inactive)
     *
     * @return List of all VIP tier details
     */
    List<VipTierDetailResponse> getAllTiers();

    /**
     * Get VIP tier by tier code
     *
     * @param tierCode Tier code (NORMAL, SILVER, GOLD, DIAMOND)
     * @return VIP tier details
     */
    VipTierDetailResponse getTierByCode(String tierCode);

    /**
     * Get VIP tier by ID
     *
     * @param tierId Tier ID
     * @return VIP tier details
     */
    VipTierDetailResponse getTierById(Long tierId);

    /**
     * Get just the image/video limits for a VIP tier.
     * Used by the frontend to render upload-quota counters without pulling the
     * full pricing payload.
     *
     * @param tierCode NORMAL/SILVER/GOLD/DIAMOND
     * @return media limit DTO (currentImages/currentVideos/remaining* are null)
     */
    VipTierMediaLimitResponse getMediaLimitsByTierCode(String tierCode);

    /**
     * Get media limits combined with the current ACTIVE image/video count on
     * the given listing, so the frontend knows how many slots are remaining.
     *
     * @param listingId target listing
     * @return media limit DTO with currentImages/currentVideos/remaining* filled in
     */
    VipTierMediaLimitResponse getMediaLimitsForListing(Long listingId);
}

