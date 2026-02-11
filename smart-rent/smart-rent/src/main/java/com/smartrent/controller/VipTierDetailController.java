package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.VipTierDetailResponse;
import com.smartrent.service.viptier.VipTierDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/vip-tiers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "VIP Tier Details", description = "APIs for retrieving VIP tier information, pricing, and features")
public class VipTierDetailController {

    VipTierDetailService vipTierDetailService;

    @GetMapping
    @Operation(
        summary = "Get all active VIP tiers",
        description = "Retrieve all active VIP tier details with pricing and features",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "VIP tiers retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = VipTierDetailResponse.class)),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "tierId": 1,
                                  "tierCode": "NORMAL",
                                  "tierName": "Tin Thường",
                                  "tierNameEn": "Normal Listing",
                                  "tierLevel": 1,
                                  "pricePerDay": 2700,
                                  "price10Days": 27000,
                                  "price15Days": 36000,
                                  "price30Days": 66000,
                                  "maxImages": 5,
                                  "maxVideos": 1,
                                  "hasBadge": false,
                                  "autoApprove": false,
                                  "noAds": false,
                                  "priorityDisplay": false,
                                  "hasShadowListing": false,
                                  "isActive": true
                                },
                                {
                                  "tierId": 2,
                                  "tierCode": "SILVER",
                                  "tierName": "VIP Bạc",
                                  "tierNameEn": "VIP Silver",
                                  "tierLevel": 2,
                                  "pricePerDay": 50000,
                                  "price10Days": 500000,
                                  "price15Days": 667500,
                                  "price30Days": 1222500,
                                  "maxImages": 10,
                                  "maxVideos": 2,
                                  "hasBadge": true,
                                  "badgeName": "VIP BẠC",
                                  "badgeColor": "blue",
                                  "autoApprove": true,
                                  "noAds": true,
                                  "priorityDisplay": true,
                                  "hasShadowListing": false,
                                  "isActive": true
                                }
                              ]
                            }
                            """
                    )
                )
            )
        }
    )
    public ApiResponse<List<VipTierDetailResponse>> getAllActiveTiers() {
        log.info("Getting all active VIP tiers");
        List<VipTierDetailResponse> tiers = vipTierDetailService.getAllActiveTiers();
        return ApiResponse.<List<VipTierDetailResponse>>builder()
                .data(tiers)
                .build();
    }

    @GetMapping("/{tierCode}")
    @Operation(
        summary = "Get VIP tier by code",
        description = "Retrieve VIP tier details by tier code (NORMAL, SILVER, GOLD, DIAMOND)",
        parameters = {
            @Parameter(
                name = "tierCode",
                description = "Tier code",
                required = true,
                example = "SILVER"
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "VIP tier retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VipTierDetailResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "VIP tier not found"
            )
        }
    )
    public ApiResponse<VipTierDetailResponse> getTierByCode(@PathVariable String tierCode) {
        log.info("Getting VIP tier by code: {}", tierCode);
        VipTierDetailResponse tier = vipTierDetailService.getTierByCode(tierCode.toUpperCase());
        return ApiResponse.<VipTierDetailResponse>builder()
                .data(tier)
                .build();
    }

    @GetMapping("/all")
    @Operation(
        summary = "Get all VIP tiers (including inactive)",
        description = "Retrieve all VIP tier details including inactive ones",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "VIP tiers retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = VipTierDetailResponse.class))
                )
            )
        }
    )
    public ApiResponse<List<VipTierDetailResponse>> getAllTiers() {
        log.info("Getting all VIP tiers");
        List<VipTierDetailResponse> tiers = vipTierDetailService.getAllTiers();
        return ApiResponse.<List<VipTierDetailResponse>>builder()
                .data(tiers)
                .build();
    }
}

