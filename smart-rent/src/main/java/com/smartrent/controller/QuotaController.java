package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.service.quota.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for quota management
 * Handles quota checking and availability queries
 */
@Slf4j
@RestController
@RequestMapping("/v1/quotas")
@Tag(name = "Quotas", description = "Quota management and availability checking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuotaController {

    QuotaService quotaService;

    @GetMapping("/check")
    @Operation(
        summary = "Check all quotas",
        description = "Check available quotas for all benefit types (Silver, Gold, Diamond posts, Boosts)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Quota information retrieved",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "All Quotas",
                        value = """
                            {
                              "data": {
                                "silverPosts": {
                                  "totalAvailable": 10,
                                  "totalUsed": 2,
                                  "totalGranted": 12
                                },
                                "goldPosts": {
                                  "totalAvailable": 5,
                                  "totalUsed": 1,
                                  "totalGranted": 6
                                },
                                "diamondPosts": {
                                  "totalAvailable": 2,
                                  "totalUsed": 0,
                                  "totalGranted": 2
                                },
                                "boosts": {
                                  "totalAvailable": 20,
                                  "totalUsed": 3,
                                  "totalGranted": 23
                                }
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, QuotaStatusResponse>> checkAllQuotas() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking all quotas for user: {}", userId);
        Map<String, QuotaStatusResponse> quotas = quotaService.checkAllQuotas(userId);

        return ApiResponse.<Map<String, QuotaStatusResponse>>builder()
                .data(quotas)
                .build();
    }

    @GetMapping("/check/{benefitType}")
    @Operation(
        summary = "Check specific quota",
        description = "Check available quota for a specific benefit type",
        parameters = {
            @Parameter(
                name = "benefitType",
                description = "Type of benefit (POST_SILVER, POST_GOLD, POST_DIAMOND, PUSH)",
                required = true
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Quota information retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QuotaStatusResponse.class),
                    examples = @ExampleObject(
                        name = "Specific Quota",
                        value = """
                            {
                              "data": {
                                "totalAvailable": 5,
                                "totalUsed": 2,
                                "totalGranted": 7
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<QuotaStatusResponse> checkQuota(@PathVariable String benefitType) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking {} quota for user: {}", benefitType, userId);
        BenefitType type = BenefitType.valueOf(benefitType);
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, type);

        return ApiResponse.<QuotaStatusResponse>builder()
                .data(quota)
                .build();
    }

    @GetMapping("/silver-posts")
    @Operation(
        summary = "Check Silver post quota",
        description = "Check available VIP Silver post quota for current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Silver post quota retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QuotaStatusResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<QuotaStatusResponse> getSilverPostQuota() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking Silver post quota for user: {}", userId);
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_SILVER);

        return ApiResponse.<QuotaStatusResponse>builder()
                .data(quota)
                .build();
    }

    @GetMapping("/gold-posts")
    @Operation(
        summary = "Check Gold post quota",
        description = "Check available VIP Gold post quota for current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Gold post quota retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QuotaStatusResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<QuotaStatusResponse> getGoldPostQuota() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking Gold post quota for user: {}", userId);
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_GOLD);

        return ApiResponse.<QuotaStatusResponse>builder()
                .data(quota)
                .build();
    }

    @GetMapping("/diamond-posts")
    @Operation(
        summary = "Check Diamond post quota",
        description = "Check available VIP Diamond post quota for current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Diamond post quota retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QuotaStatusResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<QuotaStatusResponse> getDiamondPostQuota() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking Diamond post quota for user: {}", userId);
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_DIAMOND);

        return ApiResponse.<QuotaStatusResponse>builder()
                .data(quota)
                .build();
    }

    @GetMapping("/pushes")
    @Operation(
        summary = "Check push quota",
        description = "Check available push quota for current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Push quota retrieved",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = QuotaStatusResponse.class)
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<QuotaStatusResponse> getPushQuota() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking push quota for user: {}", userId);
        QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);

        return ApiResponse.<QuotaStatusResponse>builder()
                .data(quota)
                .build();
    }

    @GetMapping("/has-membership")
    @Operation(
        summary = "Check if user has active membership",
        description = "Check if user has any active membership with quotas",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Membership status retrieved",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "data": {
                                "hasActiveMembership": true
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, Boolean>> hasActiveMembership() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Checking active membership for user: {}", userId);
        boolean hasActive = quotaService.hasActiveMembership(userId);

        return ApiResponse.<Map<String, Boolean>>builder()
                .data(Map.of("hasActiveMembership", hasActive))
                .build();
    }
}

