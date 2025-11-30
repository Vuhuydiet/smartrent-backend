package com.smartrent.service.quota;

import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.infra.repository.entity.UserMembershipBenefit;

import java.util.Set;

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
     * Consume quota from specific user membership benefits
     * @param userId User ID
     * @param benefitIds Set of UserMembershipBenefit IDs to consume from
     * @param expectedBenefitType Expected benefit type for validation
     * @return true if quota was consumed successfully from all specified benefits
     * @throws IllegalArgumentException if benefitIds are invalid or don't belong to user
     * @throws IllegalStateException if any benefit has no available quota
     */
    boolean consumeQuotaByBenefitIds(String userId, Set<Long> benefitIds, BenefitType expectedBenefitType);

    /**
     * Get user membership benefit by ID with validation
     * @param userId User ID for ownership validation
     * @param benefitId UserMembershipBenefit ID
     * @return UserMembershipBenefit entity
     * @throws IllegalArgumentException if benefit not found or doesn't belong to user
     */
    UserMembershipBenefit getBenefitById(String userId, Long benefitId);

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

