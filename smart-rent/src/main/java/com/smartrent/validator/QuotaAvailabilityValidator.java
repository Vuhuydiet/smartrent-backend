package com.smartrent.validator;

import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.service.quota.QuotaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for quota availability
 * Checks if user has sufficient quota before allowing quota-based actions
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuotaAvailabilityValidator {

    QuotaService quotaService;

    /**
     * Check if user has Silver post quota
     */
    public boolean hasSilverPostQuota(String userId) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_SILVER);
        boolean hasQuota = quota.getTotalAvailable() > 0;

        if (!hasQuota) {
            log.info("User {} has no Silver post quota available", userId);
        }

        return hasQuota;
    }

    /**
     * Check if user has Gold post quota
     */
    public boolean hasGoldPostQuota(String userId) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_GOLD);
        boolean hasQuota = quota.getTotalAvailable() > 0;

        if (!hasQuota) {
            log.info("User {} has no Gold post quota available", userId);
        }

        return hasQuota;
    }

    /**
     * Check if user has Diamond post quota
     */
    public boolean hasDiamondPostQuota(String userId) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_DIAMOND);
        boolean hasQuota = quota.getTotalAvailable() > 0;

        if (!hasQuota) {
            log.info("User {} has no Diamond post quota available", userId);
        }

        return hasQuota;
    }

    /**
     * Check if user has push quota
     */
    public boolean hasPushQuota(String userId) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);
        boolean hasQuota = quota.getTotalAvailable() > 0;

        if (!hasQuota) {
            log.info("User {} has no push quota available", userId);
        }

        return hasQuota;
    }

    /**
     * Check if user has sufficient quota for a specific benefit type
     */
    public boolean hasSufficientQuota(String userId, BenefitType benefitType, int requiredQuantity) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, benefitType);
        boolean hasSufficient = quota.getTotalAvailable() >= requiredQuantity;

        if (!hasSufficient) {
            log.info("User {} has insufficient {} quota. Required: {}, Available: {}",
                    userId, benefitType, requiredQuantity, quota.getTotalAvailable());
        }

        return hasSufficient;
    }

    /**
     * Check if user has active membership
     */
    public boolean hasActiveMembership(String userId) {
        boolean hasActive = quotaService.hasActiveMembership(userId);

        if (!hasActive) {
            log.info("User {} has no active membership", userId);
        }

        return hasActive;
    }

    /**
     * Get available quota for a benefit type
     */
    public int getAvailableQuota(String userId, BenefitType benefitType) {
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, benefitType);
        return quota.getTotalAvailable();
    }
}

