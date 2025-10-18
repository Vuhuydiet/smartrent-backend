package com.smartrent.service.quota.impl;

import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.infra.repository.UserMembershipBenefitRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.infra.repository.entity.UserMembershipBenefit;
import com.smartrent.service.quota.QuotaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of QuotaService
 * Manages user quotas from membership benefits
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuotaServiceImpl implements QuotaService {

    UserMembershipBenefitRepository userBenefitRepository;
    UserMembershipRepository userMembershipRepository;

    @Override
    @Transactional(readOnly = true)
    public QuotaStatusResponse checkQuotaAvailability(String userId, BenefitType benefitType) {
        log.info("Checking quota availability for user {} and benefit type {}", userId, benefitType);

        Integer totalAvailable = userBenefitRepository.getTotalAvailableQuota(userId, benefitType, LocalDateTime.now());
        boolean hasActiveMembership = hasActiveMembership(userId);

        if (!hasActiveMembership) {
            log.info("User {} has no active membership", userId);
            return QuotaStatusResponse.builder()
                    .totalAvailable(0)
                    .totalUsed(0)
                    .totalGranted(0)
                    .build();
        }

        Integer totalGranted = userBenefitRepository.getTotalGrantedQuota(userId, benefitType, LocalDateTime.now());
        Integer totalUsed = userBenefitRepository.getTotalUsedQuota(userId, benefitType, LocalDateTime.now());

        return QuotaStatusResponse.builder()
                .totalAvailable(totalAvailable != null ? totalAvailable : 0)
                .totalUsed(totalUsed != null ? totalUsed : 0)
                .totalGranted(totalGranted != null ? totalGranted : 0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, QuotaStatusResponse> checkAllQuotas(String userId) {
        log.info("Checking all quotas for user {}", userId);

        Map<String, QuotaStatusResponse> quotas = new HashMap<>();

        quotas.put("silverPosts", checkQuotaAvailability(userId, BenefitType.POST_SILVER));
        quotas.put("goldPosts", checkQuotaAvailability(userId, BenefitType.POST_GOLD));
        quotas.put("diamondPosts", checkQuotaAvailability(userId, BenefitType.POST_DIAMOND));
        quotas.put("pushes", checkQuotaAvailability(userId, BenefitType.PUSH));

        return quotas;
    }

    @Override
    @Transactional
    public boolean consumeQuota(String userId, BenefitType benefitType, int quantity) {
        log.info("Consuming {} quota of type {} for user {}", quantity, benefitType, userId);

        // Find first available benefit with quota
        UserMembershipBenefit benefit = userBenefitRepository
                .findFirstAvailableBenefit(userId, benefitType, LocalDateTime.now())
                .orElse(null);

        if (benefit == null) {
            log.warn("No available quota for user {} and benefit type {}", userId, benefitType);
            return false;
        }

        try {
            benefit.consumeQuota(quantity);
            userBenefitRepository.save(benefit);
            log.info("Successfully consumed {} quota for user {}", quantity, userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to consume quota: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientQuota(String userId, BenefitType benefitType, int quantity) {
        QuotaStatusResponse quota = checkQuotaAvailability(userId, benefitType);
        return quota.getTotalAvailable() >= quantity;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuota(String userId, BenefitType benefitType) {
        QuotaStatusResponse quota = checkQuotaAvailability(userId, benefitType);
        return quota.getTotalAvailable();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(String userId) {
        return userMembershipRepository.hasActiveMembership(userId, LocalDateTime.now());
    }
}

