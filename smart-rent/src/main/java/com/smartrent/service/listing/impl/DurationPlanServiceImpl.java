package com.smartrent.service.listing.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.DurationPlanRequest;
import com.smartrent.dto.response.ListingDurationPlanResponse;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.ListingDurationPlanRepository;
import com.smartrent.infra.repository.entity.ListingDurationPlan;
import com.smartrent.service.listing.DurationPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DurationPlanServiceImpl implements DurationPlanService {

    private final ListingDurationPlanRepository listingDurationPlanRepository;

    @Override
    public List<ListingDurationPlanResponse> getAllPlans() {
        log.info("Admin fetching all duration plans (including inactive)");

        List<ListingDurationPlan> plans = listingDurationPlanRepository.findAll();

        return plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ListingDurationPlanResponse getPlanById(Long planId) {
        log.info("Admin fetching duration plan by ID: {}", planId);

        ListingDurationPlan plan = listingDurationPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(DomainCode.DURATION_PLAN_NOT_FOUND,
                        "Duration plan not found with ID: " + planId));

        return mapToResponse(plan);
    }

    @Override
    @Transactional
    public ListingDurationPlanResponse createPlan(DurationPlanRequest request, String adminId) {
        log.info("Admin {} creating new duration plan: {} days", adminId, request.getDurationDays());

        // Validate duration doesn't already exist
        if (listingDurationPlanRepository.findByDurationDays(request.getDurationDays()).isPresent()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Duration plan already exists for " + request.getDurationDays() + " days");
        }

        // Create new plan
        ListingDurationPlan plan = ListingDurationPlan.builder()
                .durationDays(request.getDurationDays())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ListingDurationPlan saved = listingDurationPlanRepository.save(plan);
        log.info("Duration plan created successfully with ID: {}", saved.getPlanId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ListingDurationPlanResponse updatePlan(Long planId, DurationPlanRequest request, String adminId) {
        log.info("Admin {} updating duration plan {}: {} days", adminId, planId, request.getDurationDays());

        ListingDurationPlan plan = listingDurationPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(DomainCode.DURATION_PLAN_NOT_FOUND,
                        "Duration plan not found with ID: " + planId));

        // If changing duration, validate new duration doesn't conflict
        if (!plan.getDurationDays().equals(request.getDurationDays())) {
            listingDurationPlanRepository.findByDurationDays(request.getDurationDays())
                    .ifPresent(existing -> {
                        if (!existing.getPlanId().equals(planId)) {
                            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                                    "Another plan already exists for " + request.getDurationDays() + " days");
                        }
                    });

            plan.setDurationDays(request.getDurationDays());
        }

        // Update active status if provided
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }

        ListingDurationPlan updated = listingDurationPlanRepository.save(plan);
        log.info("Duration plan {} updated successfully", planId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId, String adminId) {
        log.info("Admin {} soft-deleting duration plan: {}", adminId, planId);

        ListingDurationPlan plan = listingDurationPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(DomainCode.DURATION_PLAN_NOT_FOUND,
                        "Duration plan not found with ID: " + planId));

        // Prevent deleting if it's the last active plan
        long activeCount = listingDurationPlanRepository.findAll().stream()
                .filter(ListingDurationPlan::getIsActive)
                .count();

        if (plan.getIsActive() && activeCount <= 1) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Cannot delete the last active duration plan. System must have at least one active plan.");
        }

        // Soft delete by setting inactive
        plan.setIsActive(false);
        listingDurationPlanRepository.save(plan);

        log.info("Duration plan {} soft-deleted (deactivated) successfully", planId);
    }

    @Override
    @Transactional
    public ListingDurationPlanResponse activatePlan(Long planId, String adminId) {
        log.info("Admin {} activating duration plan: {}", adminId, planId);

        ListingDurationPlan plan = listingDurationPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(DomainCode.DURATION_PLAN_NOT_FOUND,
                        "Duration plan not found with ID: " + planId));

        plan.setIsActive(true);
        ListingDurationPlan updated = listingDurationPlanRepository.save(plan);

        log.info("Duration plan {} activated successfully", planId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ListingDurationPlanResponse deactivatePlan(Long planId, String adminId) {
        log.info("Admin {} deactivating duration plan: {}", adminId, planId);

        ListingDurationPlan plan = listingDurationPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(DomainCode.DURATION_PLAN_NOT_FOUND,
                        "Duration plan not found with ID: " + planId));

        // Prevent deactivating if it's the last active plan
        long activeCount = listingDurationPlanRepository.findAll().stream()
                .filter(ListingDurationPlan::getIsActive)
                .count();

        if (plan.getIsActive() && activeCount <= 1) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Cannot deactivate the last active duration plan. System must have at least one active plan.");
        }

        plan.setIsActive(false);
        ListingDurationPlan updated = listingDurationPlanRepository.save(plan);

        log.info("Duration plan {} deactivated successfully", planId);
        return mapToResponse(updated);
    }

    /**
     * Map entity to response DTO with calculated prices
     */
    private ListingDurationPlanResponse mapToResponse(ListingDurationPlan plan) {
        int days = plan.getDurationDays();
        BigDecimal discount = PricingConstants.getDiscountForDuration(days);

        return ListingDurationPlanResponse.builder()
                .planId(plan.getPlanId())
                .durationDays(days)
                .isActive(plan.getIsActive())
                .discountPercentage(discount)
                .discountDescription(formatDiscountDescription(discount))
                .normalPrice(PricingConstants.calculateNormalPostPrice(days))
                .silverPrice(PricingConstants.calculateSilverPostPrice(days))
                .goldPrice(PricingConstants.calculateGoldPostPrice(days))
                .diamondPrice(PricingConstants.calculateDiamondPostPrice(days))
                .build();
    }

    private String formatDiscountDescription(BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            return "No discount";
        }
        BigDecimal percentage = discount.multiply(new BigDecimal("100"));
        return percentage.stripTrailingZeros().toPlainString() + "% off";
    }
}
