package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PushDetailResponse;
import com.smartrent.service.pushdetail.PushDetailService;
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
@RequestMapping("/v1/push-details")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Push Details", description = "APIs for retrieving push pricing information and packages")
public class PushDetailController {

    PushDetailService pushDetailService;

    @GetMapping
    @Operation(
        summary = "Get all active push details",
        description = "Retrieve all active push pricing details and packages",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Push details retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PushDetailResponse.class)),
                    examples = @ExampleObject(
                        name = "Success Response",
                        value = """
                            {
                              "code": "999999",
                              "message": null,
                              "data": [
                                {
                                  "pushDetailId": 1,
                                  "detailCode": "SINGLE_PUSH",
                                  "detailName": "Đẩy tin đơn lẻ",
                                  "detailNameEn": "Single Push",
                                  "pricePerPush": 40000,
                                  "quantity": 1,
                                  "totalPrice": 40000,
                                  "discountPercentage": 0.00,
                                  "savings": 0,
                                  "isActive": true
                                },
                                {
                                  "pushDetailId": 2,
                                  "detailCode": "PUSH_PACKAGE_3",
                                  "detailName": "Gói 3 lượt đẩy tin",
                                  "detailNameEn": "Push Package 3",
                                  "pricePerPush": 38000,
                                  "quantity": 3,
                                  "totalPrice": 114000,
                                  "discountPercentage": 5.00,
                                  "savings": 6000,
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
    public ApiResponse<List<PushDetailResponse>> getAllActiveDetails() {
        log.info("Getting all active push details");
        List<PushDetailResponse> details = pushDetailService.getAllActiveDetails();
        return ApiResponse.<List<PushDetailResponse>>builder()
                .data(details)
                .build();
    }

    @GetMapping("/{detailCode}")
    @Operation(
        summary = "Get push detail by code",
        description = "Retrieve push detail by detail code (SINGLE_PUSH, PUSH_PACKAGE_3, etc.)",
        parameters = {
            @Parameter(
                name = "detailCode",
                description = "Detail code",
                required = true,
                example = "SINGLE_PUSH"
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Push detail retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PushDetailResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Push detail not found"
            )
        }
    )
    public ApiResponse<PushDetailResponse> getDetailByCode(@PathVariable String detailCode) {
        log.info("Getting push detail by code: {}", detailCode);
        PushDetailResponse detail = pushDetailService.getDetailByCode(detailCode.toUpperCase());
        return ApiResponse.<PushDetailResponse>builder()
                .data(detail)
                .build();
    }

    @GetMapping("/all")
    @Operation(
        summary = "Get all push details (including inactive)",
        description = "Retrieve all push pricing details including inactive ones",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Push details retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PushDetailResponse.class))
                )
            )
        }
    )
    public ApiResponse<List<PushDetailResponse>> getAllDetails() {
        log.info("Getting all push details");
        List<PushDetailResponse> details = pushDetailService.getAllDetails();
        return ApiResponse.<List<PushDetailResponse>>builder()
                .data(details)
                .build();
    }
}

