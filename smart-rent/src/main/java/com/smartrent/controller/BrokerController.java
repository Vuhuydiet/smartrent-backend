package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.service.broker.BrokerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users/broker")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Broker Registration", description = "APIs for users to submit and check broker registration status")
public class BrokerController {

    BrokerService brokerService;

    @PostMapping("/register")
    @Operation(
            summary = "Submit broker registration request",
            description = """
                    Authenticated user submits a broker registration request.

                    **Idempotent behavior:**
                    - If already `APPROVED` → returns current status without changes.
                    - If already `PENDING` → returns current status without creating a duplicate request.
                    - If `NONE` or `REJECTED` → transitions to `PENDING`.

                    Admin will manually verify using: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Broker registration submitted (or already submitted)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Registration Submitted",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                "isBroker": false,
                                                "brokerVerificationStatus": "PENDING",
                                                "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                "brokerVerifiedAt": null,
                                                "brokerRejectionReason": null,
                                                "brokerVerificationSource": null
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized – authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"code":"5001","message":"Unauthenticated","data":null}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"code":"4001","message":"User not found","data":null}
                                    """))
            )
    })
    public ApiResponse<BrokerStatusResponse> registerBroker() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        BrokerStatusResponse response = brokerService.registerBroker(userId);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get current broker registration status",
            description = "Returns the authenticated user's current broker registration status, including any rejection reason.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Broker status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Status Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "userId": "user-123e4567-e89b-12d3-a456-426614174000",
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ApiResponse<BrokerStatusResponse> getBrokerStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        BrokerStatusResponse response = brokerService.getBrokerStatus(userId);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }
}
