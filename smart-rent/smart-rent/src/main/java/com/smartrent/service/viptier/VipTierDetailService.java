package com.smartrent.service.viptier;

import com.smartrent.dto.response.VipTierDetailResponse;

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
}

