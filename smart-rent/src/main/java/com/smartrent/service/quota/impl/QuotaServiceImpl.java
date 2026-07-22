package com.smartrent.service.quota.impl;

import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitStatus;
import com.smartrent.enums.BenefitType;
import com.smartrent.infra.repository.UserMembershipBenefitRepository;
import com.smartrent.infra.repository.UserMembershipRepository;
import com.smartrent.infra.repository.entity.UserMembership;
import com.smartrent.infra.repository.entity.UserMembershipBenefit;
import com.smartrent.service.quota.QuotaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

        LocalDateTime now = LocalDateTime.now();

        // Scope quota to THE single current membership — the exact slot getMyMembership
        // returns (findActiveUserMembership: latest endDate, startDate<=now<endDate).
        // Summing across every active membership (the old behaviour) meant that if two
        // slots were ever valid at once — legacy stacked purchases, an overlap during
        // upgrade/renewal, a race — their quotas ADDED UP, so check/PUSH reported far
        // more than the current package actually grants. Reading from one slot makes the
        // number always match what the user sees on their membership card and can never
        // accumulate, regardless of stale rows in the table.
        UserMembership current = userMembershipRepository.findActiveUserMembership(userId, now).orElse(null);
        if (current == null) {
            log.info("User {} has no active membership", userId);
            return QuotaStatusResponse.builder()
                    .totalAvailable(0)
                    .totalUsed(0)
                    .totalGranted(0)
                    .build();
        }

        int totalGranted = 0;
        int totalUsed = 0;
        for (UserMembershipBenefit benefit : currentBenefitsOfType(current, benefitType, now)) {
            totalGranted += benefit.getTotalQuantity();
            totalUsed += benefit.getQuantityUsed();
        }

        return QuotaStatusResponse.builder()
                .totalAvailable(totalGranted - totalUsed)
                .totalUsed(totalUsed)
                .totalGranted(totalGranted)
                .build();
    }

    /**
     * Benefits of a given type belonging to one membership slot that are still usable
     * right now (ACTIVE status, not past their own expiry). Shared by the availability
     * read and the consume path so both always agree on the same set of rows.
     */
    private List<UserMembershipBenefit> currentBenefitsOfType(UserMembership membership, BenefitType benefitType, LocalDateTime now) {
        return userBenefitRepository
                .findByUserMembershipUserMembershipId(membership.getUserMembershipId())
                .stream()
                .filter(b -> b.getBenefitType() == benefitType)
                .filter(b -> b.getStatus() == BenefitStatus.ACTIVE)
                .filter(b -> b.getExpiresAt().isAfter(now))
                .toList();
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
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean consumeQuota(String userId, BenefitType benefitType, int quantity) {
        log.info("Consuming {} quota of type {} for user {}", quantity, benefitType, userId);

        LocalDateTime now = LocalDateTime.now();

        // Consume from THE current membership only — same slot and same row set the
        // availability read uses (currentBenefitsOfType), so what we decrement is always
        // exactly what check/PUSH counted. Picking the benefit expiring soonest first
        // drains near-expiry quota before it's lost.
        UserMembership current = userMembershipRepository.findActiveUserMembership(userId, now).orElse(null);
        UserMembershipBenefit benefit = current == null ? null
                : currentBenefitsOfType(current, benefitType, now).stream()
                        .filter(UserMembershipBenefit::hasQuotaAvailable)
                        .min(Comparator.comparing(UserMembershipBenefit::getExpiresAt))
                        .orElse(null);

        if (benefit == null) {
            log.warn("No available quota for user {} and benefit type {}", userId, benefitType);
            return false;
        }

        log.debug("Found benefit {} with remaining quota: {}", benefit.getUserBenefitId(), benefit.getQuantityRemaining());

        try {
            // Consume quota
            benefit.consumeQuota(quantity);

            // Save and flush to ensure changes are persisted immediately
            userBenefitRepository.saveAndFlush(benefit);

            log.info("Successfully consumed {} quota for user {}. Benefit ID: {}, New quantity used: {}, Remaining: {}",
                    quantity, userId, benefit.getUserBenefitId(), benefit.getQuantityUsed(), benefit.getQuantityRemaining());
            return true;
        } catch (IllegalStateException e) {
            log.error("Failed to consume quota - insufficient quota: userId={}, benefitType={}, quantity={}, error={}",
                    userId, benefitType, quantity, e.getMessage(), e);
            return false;
        } catch (IllegalArgumentException e) {
            log.error("Failed to consume quota - invalid argument: userId={}, benefitType={}, quantity={}, error={}",
                    userId, benefitType, quantity, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to consume quota - unexpected error: userId={}, benefitType={}, quantity={}, error={}",
                    userId, benefitType, quantity, e.getMessage(), e);
            throw new RuntimeException("Failed to consume quota: " + e.getMessage(), e);
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
    public java.util.Optional<UserMembershipBenefit> findSpendableBenefit(String userId, Long benefitId) {
        if (benefitId == null) {
            return java.util.Optional.empty();
        }
        return userBenefitRepository.findById(benefitId)
                .filter(benefit -> benefit.getUserId().equals(userId))
                .filter(benefit -> !benefit.isExpired())
                .filter(UserMembershipBenefit::hasQuotaAvailable);
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

