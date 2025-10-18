package com.smartrent.service.quota;

import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitType;

/**
 * Service for managing user quotas
 * Handles quota checking, consumption, and availability
 */
public interface QuotaService {

    /**
     * Check quota availability for a specific benefit type
     * @param userId User ID
     * @param benefitType Type of benefit to check
     * @return Quota status with available, used, and granted counts
     */
    QuotaStatusResponse checkQuotaAvailability(String userId, BenefitType benefitType);

    /**
     * Check all quota types for a user
     * @param userId User ID
     * @return Map of all quota statuses
     */
    java.util.Map<String, QuotaStatusResponse> checkAllQuotas(String userId);

    /**
     * Consume quota for a specific benefit
     * @param userId User ID
     * @param benefitType Type of benefit to consume
     * @param quantity Quantity to consume
     * @return true if quota was consumed successfully, false if insufficient quota
     */
    boolean consumeQuota(String userId, BenefitType benefitType, int quantity);

    /**
     * Check if user has sufficient quota
     * @param userId User ID
     * @param benefitType Type of benefit to check
     * @param quantity Required quantity
     * @return true if user has sufficient quota
     */
    boolean hasSufficientQuota(String userId, BenefitType benefitType, int quantity);

    /**
     * Get available quota count for a specific benefit type
     * @param userId User ID
     * @param benefitType Type of benefit
     * @return Available quota count
     */
    int getAvailableQuota(String userId, BenefitType benefitType);

    /**
     * Check if user has any active membership with quotas
     * @param userId User ID
     * @return true if user has active membership
     */
    boolean hasActiveMembership(String userId);
}

