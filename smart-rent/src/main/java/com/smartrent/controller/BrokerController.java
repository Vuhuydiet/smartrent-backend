package com.smartrent.controller;

import com.smartrent.dto.request.BrokerRegisterRequest;
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
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            summary = "Submit broker registration request with identity documents",
            description = """
                    Authenticated user submits a broker registration request.

                    **Before calling this endpoint, upload 3 identity document images:**
                    1. `POST /v1/media/upload-url` with `purpose=BROKER_DOCUMENT, mediaType=IMAGE` — repeat ×3
                    2. `PUT <presignedUrl>` — upload each file directly
                    3. `POST /v1/media/{mediaId}/confirm` — confirm each upload ×3
                    4. Call this endpoint with the 3 confirmed `mediaId`s in the body

                    **Required documents:**
                    - `cccdFrontMediaId` — CCCD (Vietnamese National ID) front side
                    - `cccdBackMediaId` — CCCD back side
                    - `certMediaId` — Practising certificate image

                    **Idempotent behavior:**
                    - Already `PENDING` → returns current status (documents are not updated)
                    - Already `APPROVED` → returns current status unchanged
                    - `NONE` or `REJECTED` → validates documents, then transitions to `PENDING`

                    Admin manually verifies at: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Three confirmed media IDs for identity/certificate images",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BrokerRegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "Registration with Documents",
                                    value = """
                                            {
                                              "cccdFrontMediaId": 101,
                                              "cccdBackMediaId": 102,
                                              "certMediaId": 103
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Broker registration submitted (or already submitted/approved — idempotent)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Registration Submitted",
                                    value = """
                                            {
                                              "code": "999999",
                                              "data": {
                                                "userId": "user-abc-123",
                                                "isBroker": false,
                                                "brokerVerificationStatus": "PENDING",
                                                "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                "brokerVerifiedAt": null,
                                                "brokerRejectionReason": null,
                                                "brokerVerificationSource": null,
                                                "cccdFrontUrl": "https://r2.example.com/users/.../broker/...jpg?sig=...",
                                                "cccdBackUrl": "https://r2.example.com/users/.../broker/...jpg?sig=...",
                                                "certUrl": "https://r2.example.com/users/.../broker/...jpg?sig=..."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Document validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {"code":"17005","message":"Broker document must be uploaded and confirmed: CCCD front","data":null}
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(mediaType = "application/json")
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Document or user not found",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ApiResponse<BrokerStatusResponse> registerBroker(
            @Valid @RequestBody BrokerRegisterRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        BrokerStatusResponse response = brokerService.registerBroker(userId, request);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get current broker registration status",
            description = "Returns the authenticated user's current broker status, including presigned download URLs for submitted documents (if any).",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Approved Status",
                                    value = """
                                            {
                                              "code": "999999",
                                              "data": {
                                                "userId": "user-abc-123",
                                                "isBroker": true,
                                                "brokerVerificationStatus": "APPROVED",
                                                "brokerRegisteredAt": "2024-01-15T10:30:00",
                                                "brokerVerifiedAt": "2024-01-16T09:00:00",
                                                "brokerRejectionReason": null,
                                                "brokerVerificationSource": "https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20",
                                                "cccdFrontUrl": "https://r2.example.com/...?sig=...",
                                                "cccdBackUrl": "https://r2.example.com/...?sig=...",
                                                "certUrl": "https://r2.example.com/...?sig=..."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "User not found", content = @Content(mediaType = "application/json"))
    })
    public ApiResponse<BrokerStatusResponse> getBrokerStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        BrokerStatusResponse response = brokerService.getBrokerStatus(userId);
        return ApiResponse.<BrokerStatusResponse>builder().data(response).build();
    }
}
