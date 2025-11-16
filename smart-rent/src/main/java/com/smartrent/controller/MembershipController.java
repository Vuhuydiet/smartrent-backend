package com.smartrent.controller;

import com.smartrent.dto.request.MembershipPackageCreateRequest;
import com.smartrent.dto.request.MembershipPackageUpdateRequest;
import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.BenefitType;
import com.smartrent.service.membership.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        description = "Returns all available membership packages with their benefits and pricing (paginated)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved packages",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "page": 1,
                                "size": 10,
                                "totalElements": 3,
                                "totalPages": 1,
                                "data": [
                                  {
                                    "membershipId": 1,
                                    "packageCode": "BASIC_1M",
                                    "packageName": "Basic Monthly",
                                    "packageLevel": "BASIC",
                                    "durationMonths": 1,
                                    "originalPrice": 100000,
                                    "salePrice": 90000,
                                    "discountPercentage": 10.00,
                                    "isActive": true,
                                    "description": "Basic membership package",
                                    "benefits": []
                                  }
                                ]
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<PageResponse<MembershipPackageResponse>> getAllPackages(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting all active membership packages - page: {}, size: {}", page, size);
        PageResponse<MembershipPackageResponse> packages = membershipService.getAllActivePackages(page, size);
        return ApiResponse.<PageResponse<MembershipPackageResponse>>builder()
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

    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create membership package (Admin operation)",
        description = "Creates a new membership package. This is an admin operation.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MembershipPackageCreateRequest.class),
                examples = @ExampleObject(
                    name = "Create Package Request",
                    value = """
                        {
                          "packageCode": "PREMIUM_12M",
                          "packageName": "Premium 12 Months",
                          "packageLevel": "ADVANCED",
                          "durationMonths": 12,
                          "originalPrice": 12000000,
                          "salePrice": 9990000,
                          "discountPercentage": 16.75,
                          "isActive": true,
                          "description": "Premium membership with all features for 12 months"
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Package created successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MembershipPackageResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Package code already exists",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Conflict Error",
                        value = """
                            {
                              "code": "3006",
                              "message": "Membership package with code PREMIUM_12M already exists"
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<MembershipPackageResponse> createPackage(
            @Valid @RequestBody MembershipPackageCreateRequest request) {
        log.info("Creating new membership package with code: {}", request.getPackageCode());
        MembershipPackageResponse response = membershipService.createPackage(request);
        return ApiResponse.<MembershipPackageResponse>builder()
                .data(response)
                .build();
    }

    @PutMapping("/packages/{membershipId}")
    @Operation(
        summary = "Update membership package (Admin operation)",
        description = "Updates an existing membership package. This is an admin operation.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MembershipPackageUpdateRequest.class),
                examples = @ExampleObject(
                    name = "Update Package Request",
                    value = """
                        {
                          "packageName": "Premium 12 Months - Updated",
                          "salePrice": 8990000,
                          "discountPercentage": 25.08,
                          "isActive": true
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Package updated successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MembershipPackageResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Package not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found Error",
                        value = """
                            {
                              "code": "4015",
                              "message": "Membership package not found"
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<MembershipPackageResponse> updatePackage(
            @Parameter(description = "Membership package ID", required = true)
            @PathVariable Long membershipId,
            @Valid @RequestBody MembershipPackageUpdateRequest request) {
        log.info("Updating membership package: {}", membershipId);
        MembershipPackageResponse response = membershipService.updatePackage(membershipId, request);
        return ApiResponse.<MembershipPackageResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping("/packages/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete membership package (Admin operation)",
        description = "Deletes a membership package. This is an admin operation.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Package deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Package not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found Error",
                        value = """
                            {
                              "code": "4015",
                              "message": "Membership package not found"
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public void deletePackage(
            @Parameter(description = "Membership package ID", required = true)
            @PathVariable Long membershipId) {
        log.info("Deleting membership package: {}", membershipId);
        membershipService.deletePackage(membershipId);
    }

    @PostMapping("/initiate-purchase")
    @Operation(
        summary = "Initiate membership purchase",
        description = "Initiate membership package purchase and receive payment URL. After successful payment, the user will be redirected to the configured frontend URL with payment result parameters. The frontend should then verify the payment status by calling GET /v1/payments/transactions/{txnRef}.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MembershipPurchaseRequest.class),
                examples = @ExampleObject(
                    name = "Purchase Request",
                    summary = "Request to purchase membership package",
                    value = """
                        {
                          "membershipId": 2,
                          "paymentProvider": "VNPAY"
                        }
                        """
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Payment URL generated successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = """
                            {
                              "code": "200000",
                              "message": "Success",
                              "data": {
                                "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
                                "transactionRef": "abc123-def456-ghi789",
                                "amount": 299000,
                                "provider": "VNPAY"
                              }
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request - Missing required field (membershipId is required) or invalid payment provider",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Validation Error",
                        value = """
                            {
                              "code": "400000",
                              "message": "Membership package ID is required"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Membership package not found or user not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Not Found Error",
                        value = """
                            {
                              "code": "404000",
                              "message": "Membership package not found: 999"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "422",
                description = "Membership package is not active",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Inactive Package Error",
                        value = """
                            {
                              "code": "422000",
                              "message": "Membership package is not active"
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<PaymentResponse> initiateMembershipPurchase(
            @RequestBody @Valid MembershipPurchaseRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} initiating membership purchase for package {}", userId, request.getMembershipId());
        PaymentResponse response = membershipService.initiateMembershipPurchase(userId, request);
        return ApiResponse.<PaymentResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping("/purchase")
    @Operation(
        summary = "Purchase a membership package (Deprecated)",
        description = "Direct purchase of membership package. This endpoint is deprecated. Use /initiate-purchase instead for payment flow.",
        deprecated = true,
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
    @Deprecated
    public ApiResponse<UserMembershipResponse> purchaseMembership(
            @RequestBody @Valid MembershipPurchaseRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} purchasing membership package {} (deprecated endpoint)", userId, request.getMembershipId());
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
    public ApiResponse<UserMembershipResponse> getMyMembership() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting active membership for user: {}", userId);
        UserMembershipResponse response = membershipService.getActiveMembership(userId);
        return ApiResponse.<UserMembershipResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/history")
    @Operation(
        summary = "Get membership history",
        description = "Returns all past and current memberships for the user (paginated)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": {
                                "page": 1,
                                "size": 10,
                                "totalElements": 5,
                                "totalPages": 1,
                                "data": [
                                  {
                                    "userMembershipId": 1,
                                    "userId": "user123",
                                    "membershipPackage": {},
                                    "startDate": "2024-01-01T00:00:00",
                                    "endDate": "2024-02-01T00:00:00",
                                    "status": "ACTIVE"
                                  }
                                ]
                              }
                            }
                            """
                    )
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<PageResponse<UserMembershipResponse>> getMembershipHistory(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting membership history for user: {} - page: {}, size: {}", userId, page, size);
        PageResponse<UserMembershipResponse> history = membershipService.getMembershipHistory(userId, page, size);
        return ApiResponse.<PageResponse<UserMembershipResponse>>builder()
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
    public ApiResponse<Void> cancelMembership(@PathVariable Long userMembershipId) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} cancelling membership {}", userId, userMembershipId);
        membershipService.cancelMembership(userId, userMembershipId);
        return ApiResponse.<Void>builder()
                .message("Membership cancelled successfully")
                .build();
    }
}

