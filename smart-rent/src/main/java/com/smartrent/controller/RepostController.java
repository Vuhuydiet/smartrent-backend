package com.smartrent.controller;

import com.smartrent.dto.request.RepostListingRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RepostResponse;
import com.smartrent.service.repost.RepostService;
import io.swagger.v3.oas.annotations.Operation;
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

@Slf4j
@RestController
@RequestMapping("/v1/reposts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Repost Management", description = "APIs for re-publishing expired listings")
public class RepostController {

    RepostService repostService;

    @PostMapping("/repost")
    @Operation(
        summary = "Repost an expired listing",
        description = """
            Re-publish an expired listing. The user can either consume the
            matching membership post-quota for the listing's vipType (e.g.
            POST_GOLD for a GOLD listing) or pay the per-day fee via VNPay.

            On a successful quota repost the listing is immediately reactivated
            (expired=false, fresh expiryDate, postDate, pushedAt). On a payment
            repost the response carries paymentUrl + transactionId — the
            listing reactivates only after the payment provider callback fires.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RepostListingRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Repost with quota",
                        summary = "Repost using membership quota",
                        value = """
                            {
                              "listingId": 123,
                              "useMembershipQuota": true,
                              "durationDays": 30
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Repost with payment",
                        summary = "Repost with direct VNPay payment",
                        value = """
                            {
                              "listingId": 123,
                              "useMembershipQuota": false,
                              "paymentProvider": "VNPAY",
                              "durationDays": 30
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully reposted listing or generated payment URL",
                content = @Content(schema = @Schema(implementation = RepostResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request, listing not expired, or insufficient quota"
            )
        }
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<RepostResponse> repostListing(@RequestBody @Valid RepostListingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("User {} reposting listing {}", userId, request.getListingId());
        RepostResponse response = repostService.repostListing(userId, request);
        return ApiResponse.<RepostResponse>builder().data(response).build();
    }
}
