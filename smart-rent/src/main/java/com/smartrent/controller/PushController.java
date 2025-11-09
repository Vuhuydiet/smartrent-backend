package com.smartrent.controller;

import com.smartrent.dto.request.PushListingRequest;
import com.smartrent.dto.request.SchedulePushRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PushResponse;
import com.smartrent.service.push.PushService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/pushes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Push Management", description = "APIs for pushing listings to increase visibility")
public class PushController {

    PushService pushService;

    @PostMapping("/push")
    @Operation(
        summary = "Push a listing",
        description = "Push a listing to the top of search results. Can use membership quota or direct purchase. If payment is required, the user will be redirected to the configured frontend URL after payment completion. Verify payment status using GET /v1/payments/transactions/{txnRef}.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PushListingRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Push with Quota",
                        summary = "Push listing using membership quota",
                        value = """
                            {
                              "listingId": 123,
                              "useMembershipQuota": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Push with Payment",
                        summary = "Push listing with direct payment",
                        value = """
                            {
                              "listingId": 123,
                              "useMembershipQuota": false,
                              "paymentProvider": "VNPAY"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully pushed listing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PushResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient quota"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<PushResponse> pushListing(@RequestBody @Valid PushListingRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} pushing listing {}", userId, request.getListingId());
        PushResponse response = pushService.pushListing(userId, request);
        return ApiResponse.<PushResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping("/schedule")
    @Operation(
        summary = "Schedule automatic pushes",
        description = "Schedule automatic daily pushes for a listing at a specific time",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully scheduled pushes",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PushResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient quota"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<PushResponse> schedulePush(@RequestBody @Valid SchedulePushRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} scheduling push for listing {}", userId, request.getListingId());
        PushResponse response = pushService.schedulePush(userId, request);
        return ApiResponse.<PushResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/listing/{listingId}/history")
    @Operation(
        summary = "Get push history for a listing",
        description = "Returns all push history for a specific listing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PushResponse.class))
                )
            )
        }
    )
    public ApiResponse<List<PushResponse>> getListingPushHistory(@PathVariable Long listingId) {
        log.info("Getting push history for listing: {}", listingId);
        List<PushResponse> history = pushService.getPushHistory(listingId);
        return ApiResponse.<List<PushResponse>>builder()
                .data(history)
                .build();
    }

    @GetMapping("/my-history")
    @Operation(
        summary = "Get user's push history",
        description = "Returns all push history for all listings owned by the current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PushResponse.class))
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<List<PushResponse>> getMyPushHistory() {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting push history for user: {}", userId);
        List<PushResponse> history = pushService.getUserPushHistory(userId);
        return ApiResponse.<List<PushResponse>>builder()
                .data(history)
                .build();
    }

    @DeleteMapping("/schedule/{scheduleId}")
    @Operation(
        summary = "Cancel scheduled push",
        description = "Cancel a scheduled automatic push",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully cancelled schedule"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Schedule not found"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Void> cancelScheduledPush(@PathVariable Long scheduleId) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} cancelling scheduled push {}", userId, scheduleId);
        pushService.cancelScheduledPush(userId, scheduleId);
        return ApiResponse.<Void>builder()
                .message("Scheduled push cancelled successfully")
                .build();
    }
}
