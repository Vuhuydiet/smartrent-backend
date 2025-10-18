package com.smartrent.controller;

import com.smartrent.dto.request.BoostListingRequest;
import com.smartrent.dto.request.ScheduleBoostRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.BoostResponse;
import com.smartrent.service.boost.BoostService;
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
@RequestMapping("/v1/boosts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Boost Management", description = "APIs for boosting listings to increase visibility")
public class BoostController {

    BoostService boostService;

    @PostMapping("/boost")
    @Operation(
        summary = "Boost a listing",
        description = "Push a listing to the top of search results. Can use membership quota or direct purchase.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully boosted listing",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BoostResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient quota"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<BoostResponse> boostListing(
            @RequestHeader("user-id") String userId,
            @RequestBody @Valid BoostListingRequest request) {
        log.info("User {} boosting listing {}", userId, request.getListingId());
        BoostResponse response = boostService.boostListing(userId, request);
        return ApiResponse.<BoostResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping("/schedule")
    @Operation(
        summary = "Schedule automatic boosts",
        description = "Schedule automatic daily boosts for a listing at a specific time",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully scheduled boosts",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BoostResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient quota"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<BoostResponse> scheduleBoost(
            @RequestHeader("user-id") String userId,
            @RequestBody @Valid ScheduleBoostRequest request) {
        log.info("User {} scheduling boost for listing {}", userId, request.getListingId());
        BoostResponse response = boostService.scheduleBoost(userId, request);
        return ApiResponse.<BoostResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping("/listing/{listingId}/history")
    @Operation(
        summary = "Get boost history for a listing",
        description = "Returns all boost history for a specific listing",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = BoostResponse.class))
                )
            )
        }
    )
    public ApiResponse<List<BoostResponse>> getListingBoostHistory(@PathVariable Long listingId) {
        log.info("Getting boost history for listing: {}", listingId);
        List<BoostResponse> history = boostService.getBoostHistory(listingId);
        return ApiResponse.<List<BoostResponse>>builder()
                .data(history)
                .build();
    }

    @GetMapping("/my-history")
    @Operation(
        summary = "Get user's boost history",
        description = "Returns all boost history for the current user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved history",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = BoostResponse.class))
                )
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<List<BoostResponse>> getMyBoostHistory(@RequestHeader("user-id") String userId) {
        log.info("Getting boost history for user: {}", userId);
        List<BoostResponse> history = boostService.getUserBoostHistory(userId);
        return ApiResponse.<List<BoostResponse>>builder()
                .data(history)
                .build();
    }

    @DeleteMapping("/schedule/{scheduleId}")
    @Operation(
        summary = "Cancel scheduled boost",
        description = "Cancel a scheduled automatic boost",
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
    public ApiResponse<Void> cancelScheduledBoost(
            @RequestHeader("user-id") String userId,
            @PathVariable Long scheduleId) {
        log.info("User {} cancelling scheduled boost {}", userId, scheduleId);
        boostService.cancelScheduledBoost(userId, scheduleId);
        return ApiResponse.<Void>builder()
                .message("Scheduled boost cancelled successfully")
                .build();
    }
}

