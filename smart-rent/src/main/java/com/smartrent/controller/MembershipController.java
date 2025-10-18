package com.smartrent.controller;

import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.BenefitType;
import com.smartrent.service.membership.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/memberships")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Membership Management", description = "APIs for managing membership packages and subscriptions")
public class MembershipController {

    MembershipService membershipService;

    @GetMapping("/packages")
    @Operation(
        summary = "Get all active membership packages",
        description = "Returns all available membership packages with their benefits and pricing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved packages",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MembershipPackageResponse.class))
                )
            )
        }
    )
    public ApiResponse<List<MembershipPackageResponse>> getAllPackages() {
        log.info("Getting all active membership packages");
        List<MembershipPackageResponse> packages = membershipService.getAllActivePackages();
        return ApiResponse.<List<MembershipPackageResponse>>builder()
                .data(packages)
                .build();
    }

    @GetMapping("/packages/{membershipId}")
    @Operation(
        summary = "Get membership package by ID",
        description = "Returns detailed information about a specific membership package",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved package",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MembershipPackageResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Package not found"
            )
        }
    )
    public ApiResponse<MembershipPackageResponse> getPackageById(@PathVariable Long membershipId) {
        log.info("Getting membership package: {}", membershipId);
        MembershipPackageResponse packageResponse = membershipService.getPackageById(membershipId);
        return ApiResponse.<MembershipPackageResponse>builder()
                .data(packageResponse)
                .build();
    }

    @PostMapping("/purchase")
    @Operation(
        summary = "Purchase a membership package",
        description = "Purchase a membership package and receive all benefits immediately",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully purchased membership",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserMembershipResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient funds"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<UserMembershipResponse> purchaseMembership(
            @RequestHeader("user-id") String userId,
            @RequestBody @Valid MembershipPurchaseRequest request) {
        log.info("User {} purchasing membership package {}", userId, request.getMembershipId());
        UserMembershipResponse response = membershipService.purchaseMembership(userId, request);
        return ApiResponse.<UserMembershipResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/my-membership")
    @Operation(
        summary = "Get current active membership",
        description = "Returns the user's current active membership with all benefits and quotas",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved membership",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserMembershipResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No active membership found"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<UserMembershipResponse> getMyMembership(@RequestHeader("user-id") String userId) {
        log.info("Getting active membership for user: {}", userId);
        UserMembershipResponse response = membershipService.getActiveMembership(userId);
        return ApiResponse.<UserMembershipResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/history")
    @Operation(
        summary = "Get membership history",
        description = "Returns all past and current memberships for the user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = UserMembershipResponse.class))
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<List<UserMembershipResponse>> getMembershipHistory(@RequestHeader("user-id") String userId) {
        log.info("Getting membership history for user: {}", userId);
        List<UserMembershipResponse> history = membershipService.getMembershipHistory(userId);
        return ApiResponse.<List<UserMembershipResponse>>builder()
                .data(history)
                .build();
    }

    @DeleteMapping("/{userMembershipId}")
    @Operation(
        summary = "Cancel membership",
        description = "Cancel an active membership and expire all benefits",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully cancelled membership"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Membership not found"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Void> cancelMembership(
            @RequestHeader("user-id") String userId,
            @PathVariable Long userMembershipId) {
        log.info("User {} cancelling membership {}", userId, userMembershipId);
        membershipService.cancelMembership(userId, userMembershipId);
        return ApiResponse.<Void>builder()
                .message("Membership cancelled successfully")
                .build();
    }
}

