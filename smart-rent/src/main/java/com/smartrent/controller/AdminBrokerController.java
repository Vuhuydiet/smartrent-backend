package com.smartrent.controller;

import com.smartrent.dto.request.BrokerVerificationRequest;
import com.smartrent.dto.response.AdminBrokerUserResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.service.broker.BrokerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - Broker Verification", description = "Admin APIs for reviewing and managing broker registrations")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminBrokerController {

    BrokerService brokerService;

    // ──────────────────────────────────────────────────────────────────
    // List pending registrations
    // ──────────────────────────────────────────────────────────────────

    @GetMapping("/broker-pending")
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
    @Operation(
            summary = "List users with PENDING broker registration (Admin only)",
            description = """
                    Returns a paginated list of users who have submitted a broker registration
                    and are awaiting admin review.

                    Results are ordered oldest-first (FIFO) so admins process requests in submission order.

                    **Admin workflow:**
                    1. Call this endpoint to find pending users.
                    2. Manually verify the user at: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
                    3. Call `PATCH /{userId}/broker-verification` to approve or reject.

                    **Permissions:** SA, UA, SPA roles only.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pending broker list retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Pending List",
                                    value = """
                                            {
                                              "code": "999999",
                                              "data": {
                                                "page": 1,
                                                "size": 20,
                                                "totalElements": 3,
                                                "totalPages": 1,
                                                "data": [
                                                  {
                                                    "userId": "user-abc-123",
                                                    "firstName": "Nguyen",
                                                    "lastName": "Van A",
                                                    "email": "nguyen.vana@example.com",
                                                    "phoneCode": "+84",
                                                    "phoneNumber": "0912345678",
                                                    "isBroker": false,
                                                    "brokerVerificationStatus": "PENDING",
                                                    "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                    "brokerVerifiedAt": null,
                                                    "brokerVerifiedByAdminId": null,
                                                    "brokerRejectionReason": null
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden – admin role required",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ApiResponse<PageResponse<AdminBrokerUserResponse>> getPendingBrokers(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<AdminBrokerUserResponse> result = brokerService.getPendingBrokers(page, size);
        return ApiResponse.<PageResponse<AdminBrokerUserResponse>>builder().data(result).build();
    }

    // ──────────────────────────────────────────────────────────────────
    // Approve / reject a registration
    // ──────────────────────────────────────────────────────────────────

    @PatchMapping("/{userId}/broker-verification")
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
    @Operation(
            summary = "Approve or reject a user's broker registration (Admin only)",
            description = """
                    Reviews a user's broker registration request.

                    **Permissions:** Super Admin (SA), User Admin (UA), and Support Admin (SPA) only.

                    **Actions:**
                    - `APPROVE` – Sets `isBroker=true`, status=`APPROVED`. Admin should have manually
                      verified the user at: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
                    - `REJECT` – Sets `isBroker=false`, status=`REJECTED`. `rejectionReason` is required.

                    The `broker_verification_source` is automatically recorded. The user receives an
                    in-app notification with the result.
                    """,
            parameters = {
                    @Parameter(name = "userId", description = "ID of the user to review", required = true,
                            example = "user-123e4567-e89b-12d3-a456-426614174000")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Broker verification decision",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BrokerVerificationRequest.class),
                            examples = {
                                    @ExampleObject(name = "Approve",
                                            value = """
                                                    { "action": "APPROVE" }
                                                    """),
                                    @ExampleObject(name = "Reject",
                                            value = """
                                                    { "action": "REJECT", "rejectionReason": "Could not verify license on external registry" }
                                                    """)
                            }
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Broker verification updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Approved",
                                    value = """
                                            {
                                              "code": "999999",
                                              "data": {
                                                "userId": "user-123",
                                                "isBroker": true,
                                                "brokerVerificationStatus": "APPROVED",
                                                "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                "brokerVerifiedAt": "2024-01-16T09:00:00",
                                                "brokerRejectionReason": null,
                                                "brokerVerificationSource": "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid action or missing rejection reason",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"code":"17001","message":"Rejection reason is required when rejecting broker verification","data":null}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden – admin role required", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "User not found", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"code":"4001","message":"User not found","data":null}
                            """)))
    })
    public ApiResponse<BrokerStatusResponse> reviewBrokerVerification(
            @PathVariable String userId,
            @Valid @RequestBody BrokerVerificationRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();

        log.info("Admin {} reviewing broker for user={}, action={}", adminId, userId, request.getAction());

        BrokerStatusResponse response = brokerService.reviewBroker(userId, adminId, request);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }

    @DeleteMapping("/{userId}/broker")
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
    @Operation(
            summary = "Remove broker role from a user (Admin only)",
            description = """
                    Removes broker privileges for a user.

                    **Permissions:** Super Admin (SA), User Admin (UA), and Support Admin (SPA) only.

                    **Effect:** Sets `isBroker=false`, updates verification status to `REJECTED`,
                    and records admin action metadata.
                    """,
            parameters = {
                    @Parameter(name = "userId", description = "ID of the user", required = true,
                            example = "user-123e4567-e89b-12d3-a456-426614174000")
            }
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Broker role removed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Broker role removed",
                                    value = """
                                            {
                                              "code": "999999",
                                              "data": {
                                                "userId": "user-123",
                                                "isBroker": false,
                                                "brokerVerificationStatus": "REJECTED",
                                                "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                "brokerVerifiedAt": "2024-01-16T09:00:00",
                                                "brokerRejectionReason": "Broker role removed by admin",
                                                "brokerVerificationSource": "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden – admin role required", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "User not found", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"code":"4001","message":"User not found","data":null}
                            """)))
    })
    public ApiResponse<BrokerStatusResponse> removeBrokerRole(@PathVariable String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();

        log.info("Admin {} removing broker role for user={}", adminId, userId);

        BrokerStatusResponse response = brokerService.removeBrokerRole(userId, adminId);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }
}
