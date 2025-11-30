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
import java.util.Set;

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
    @Transactional
    public boolean consumeQuotaByBenefitIds(String userId, Set<Long> benefitIds, BenefitType expectedBenefitType) {
        log.info("Consuming quota from specific benefits {} for user {} with expected type {}",
                benefitIds, userId, expectedBenefitType);

        if (benefitIds == null || benefitIds.isEmpty()) {
            log.warn("No benefit IDs provided for user {}", userId);
            throw new IllegalArgumentException("Benefit IDs cannot be null or empty");
        }

        for (Long benefitId : benefitIds) {
            UserMembershipBenefit benefit = getBenefitById(userId, benefitId);

            // Validate benefit type matches expected type
            if (benefit.getBenefitType() != expectedBenefitType) {
                log.error("Benefit {} has type {} but expected {}", benefitId, benefit.getBenefitType(), expectedBenefitType);
                throw new IllegalArgumentException(
                        String.format("Benefit %d has type %s but expected %s",
                                benefitId, benefit.getBenefitType(), expectedBenefitType));
            }

            // Check if benefit has available quota
            if (!benefit.hasQuotaAvailable()) {
                log.error("Benefit {} has no available quota", benefitId);
                throw new IllegalStateException(
                        String.format("Benefit %d has no available quota. Remaining: %d",
                                benefitId, benefit.getQuantityRemaining()));
            }

            // Consume 1 quota from this benefit
            try {
                benefit.consumeQuota(1);
                userBenefitRepository.save(benefit);
                log.info("Successfully consumed 1 quota from benefit {} for user {}", benefitId, userId);
            } catch (Exception e) {
                log.error("Failed to consume quota from benefit {}: {}", benefitId, e.getMessage());
                throw new IllegalStateException("Failed to consume quota from benefit " + benefitId + ": " + e.getMessage());
            }
        }

        log.info("Successfully consumed quota from all {} benefits for user {}", benefitIds.size(), userId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserMembershipBenefit getBenefitById(String userId, Long benefitId) {
        log.debug("Getting benefit {} for user {}", benefitId, userId);

        UserMembershipBenefit benefit = userBenefitRepository.findById(benefitId)
                .orElseThrow(() -> {
                    log.error("Benefit {} not found", benefitId);
                    return new IllegalArgumentException("Benefit not found with ID: " + benefitId);
                });

        // Validate ownership
        if (!benefit.getUserId().equals(userId)) {
            log.error("Benefit {} does not belong to user {}", benefitId, userId);
            throw new IllegalArgumentException(
                    String.format("Benefit %d does not belong to user %s", benefitId, userId));
        }

        // Validate benefit is not expired
        if (benefit.isExpired()) {
            log.error("Benefit {} is expired", benefitId);
            throw new IllegalStateException(
                    String.format("Benefit %d is expired", benefitId));
        }

        return benefit;
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

