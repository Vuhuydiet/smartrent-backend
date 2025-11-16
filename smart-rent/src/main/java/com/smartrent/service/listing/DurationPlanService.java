package com.smartrent.service.listing;

import com.smartrent.dto.request.DurationPlanRequest;
import com.smartrent.dto.response.ListingDurationPlanResponse;

import java.util.List;

public interface DurationPlanService {

    /**
     * Get all duration plans (including inactive) - Admin only
     */
    List<ListingDurationPlanResponse> getAllPlans();

    /**
     * Get duration plan by ID
     */
    ListingDurationPlanResponse getPlanById(Long planId);

    /**
     * Create new duration plan
     */
    ListingDurationPlanResponse createPlan(DurationPlanRequest request, String adminId);

    /**
     * Update duration plan
     */
    ListingDurationPlanResponse updatePlan(Long planId, DurationPlanRequest request, String adminId);

    /**
     * Soft delete (deactivate) duration plan
     */
    void deletePlan(Long planId, String adminId);

    /**
     * Activate duration plan
     */
    ListingDurationPlanResponse activatePlan(Long planId, String adminId);

    /**
     * Deactivate duration plan
     */
    ListingDurationPlanResponse deactivatePlan(Long planId, String adminId);
}
