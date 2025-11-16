package com.smartrent.controller;

import com.smartrent.dto.request.DurationPlanRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingDurationPlanResponse;
import com.smartrent.service.listing.DurationPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/duration-plans")
@Tag(name = "Duration Plans Management (Admin)", description = "Admin APIs for managing listing duration plans")
@RequiredArgsConstructor
public class DurationPlanAdminController {

    private final DurationPlanService durationPlanService;

    @GetMapping
    @Operation(
        summary = "Get all duration plans (including inactive)",
        description = """
            Retrieves all duration plans regardless of active status.
            Admin-only endpoint to view all plans in the system.
            """
    )
    public ApiResponse<List<ListingDurationPlanResponse>> getAllPlans(
            @RequestHeader("X-Admin-Id") String adminId) {

        List<ListingDurationPlanResponse> plans = durationPlanService.getAllPlans();

        return ApiResponse.<List<ListingDurationPlanResponse>>builder()
                .code("200000")
                .message("All duration plans retrieved successfully")
                .data(plans)
                .build();
    }

    @GetMapping("/{planId}")
    @Operation(
        summary = "Get duration plan by ID",
        description = "Retrieves a specific duration plan by its ID."
    )
    public ApiResponse<ListingDurationPlanResponse> getPlanById(
            @PathVariable Long planId,
            @RequestHeader("X-Admin-Id") String adminId) {

        ListingDurationPlanResponse plan = durationPlanService.getPlanById(planId);

        return ApiResponse.<ListingDurationPlanResponse>builder()
                .code("200000")
                .message("Duration plan retrieved successfully")
                .data(plan)
                .build();
    }

    @PostMapping
    @Operation(
        summary = "Create new duration plan",
        description = """
            Creates a new duration plan with specified number of days.

            **Validation:**
            - Duration must be positive
            - Duration must be unique (no duplicate plans)
            - Plan is created as active by default

            **Example:** Create a 45-day plan for special promotions
            """
    )
    public ApiResponse<ListingDurationPlanResponse> createPlan(
            @Valid @RequestBody DurationPlanRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {

        ListingDurationPlanResponse plan = durationPlanService.createPlan(request, adminId);

        return ApiResponse.<ListingDurationPlanResponse>builder()
                .code("200000")
                .message("Duration plan created successfully")
                .data(plan)
                .build();
    }

    @PutMapping("/{planId}")
    @Operation(
        summary = "Update duration plan",
        description = """
            Updates an existing duration plan.

            **What can be updated:**
            - Duration days (must remain unique)
            - Active status

            **Note:** Updating duration affects all future listings using this plan.
            Existing listings keep their original duration.
            """
    )
    public ApiResponse<ListingDurationPlanResponse> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody DurationPlanRequest request,
            @RequestHeader("X-Admin-Id") String adminId) {

        ListingDurationPlanResponse plan = durationPlanService.updatePlan(planId, request, adminId);

        return ApiResponse.<ListingDurationPlanResponse>builder()
                .code("200000")
                .message("Duration plan updated successfully")
                .data(plan)
                .build();
    }

    @DeleteMapping("/{planId}")
    @Operation(
        summary = "Delete duration plan",
        description = """
            Soft-deletes a duration plan by setting it to inactive.

            **Behavior:**
            - Plan is marked as inactive (not truly deleted)
            - No longer appears in public listing creation APIs
            - Still visible in admin panel
            - Existing listings using this plan are not affected

            **Note:** Cannot delete if it's the last active plan
            """
    )
    public ApiResponse<Void> deletePlan(
            @PathVariable Long planId,
            @RequestHeader("X-Admin-Id") String adminId) {

        durationPlanService.deletePlan(planId, adminId);

        return ApiResponse.<Void>builder()
                .code("200000")
                .message("Duration plan deleted successfully")
                .build();
    }

    @PatchMapping("/{planId}/activate")
    @Operation(
        summary = "Activate duration plan",
        description = "Reactivates a previously deactivated duration plan."
    )
    public ApiResponse<ListingDurationPlanResponse> activatePlan(
            @PathVariable Long planId,
            @RequestHeader("X-Admin-Id") String adminId) {

        ListingDurationPlanResponse plan = durationPlanService.activatePlan(planId, adminId);

        return ApiResponse.<ListingDurationPlanResponse>builder()
                .code("200000")
                .message("Duration plan activated successfully")
                .data(plan)
                .build();
    }

    @PatchMapping("/{planId}/deactivate")
    @Operation(
        summary = "Deactivate duration plan",
        description = """
            Deactivates a duration plan without deleting it.

            **Effect:**
            - Plan hidden from public APIs
            - Cannot be selected when creating new listings
            - Can be reactivated later

            **Note:** Cannot deactivate if it's the last active plan
            """
    )
    public ApiResponse<ListingDurationPlanResponse> deactivatePlan(
            @PathVariable Long planId,
            @RequestHeader("X-Admin-Id") String adminId) {

        ListingDurationPlanResponse plan = durationPlanService.deactivatePlan(planId, adminId);

        return ApiResponse.<ListingDurationPlanResponse>builder()
                .code("200000")
                .message("Duration plan deactivated successfully")
                .data(plan)
                .build();
    }
}
