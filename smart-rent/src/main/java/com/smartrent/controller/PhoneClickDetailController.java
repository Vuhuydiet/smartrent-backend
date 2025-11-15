package com.smartrent.controller;

import com.smartrent.dto.request.PhoneClickRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PhoneClickResponse;
import com.smartrent.dto.response.PhoneClickStatsResponse;
import com.smartrent.service.phoneclickdetail.PhoneClickDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/phone-click-details")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
        name = "Phone Click Detail Tracking",
        description = """
                APIs for tracking and managing phone number clicks on listings.
                
                **Features:**
                - Track when users click on phone numbers in listing details
                - Prompt users to input their contact phone if not provided
                - View users who are interested in your listings
                - Get statistics about phone clicks
                
                **Use Cases:**
                - User clicks phone number → System tracks interest
                - Renter views listing management → See who clicked phone numbers
                - Analytics and engagement tracking
                """
)
public class PhoneClickDetailController {

    PhoneClickDetailService phoneClickDetailService;

    @PostMapping
    @Operation(
            summary = "Track phone number click",
            description = """
                    Track when a user clicks on a phone number in a listing detail page.
                    
                    **Behavior:**
                    - Requires authentication (user must be logged in)
                    - If user doesn't have contact phone, frontend should prompt for it
                    - Records the click with timestamp, IP, and user agent
                    - Multiple clicks by same user are tracked separately
                    
                    **Returns:**
                    - Phone click record with user details
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Phone click tracking request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PhoneClickRequest.class),
                            examples = @ExampleObject(
                                    name = "Track Phone Click",
                                    value = """
                                            {
                                              "listingId": 123
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Phone click tracked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    public ApiResponse<PhoneClickResponse> trackPhoneClick(
            @Valid @RequestBody PhoneClickRequest request,
            HttpServletRequest httpRequest
    ) {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Extract IP address
        String ipAddress = extractIpAddress(httpRequest);
        
        // Extract user agent
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Tracking phone click for listing {} by user {} from IP {}", 
                request.getListingId(), userId, ipAddress);

        PhoneClickResponse response = phoneClickDetailService.trackPhoneClick(
                request, userId, ipAddress, userAgent);

        return ApiResponse.<PhoneClickResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/listing/{listingId}")
    @Operation(
            summary = "Get users who clicked on listing's phone number",
            description = """
                    Get all users who clicked on a specific listing's phone number.
                    Returns unique users with their contact details.
                    
                    **Use Case:**
                    - Renter views listing management page
                    - See which users are interested in the listing
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved phone clicks",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    public ApiResponse<List<PhoneClickResponse>> getPhoneClicksByListing(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId
    ) {
        log.info("Getting phone clicks for listing {}", listingId);

        List<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksByListing(listingId);

        return ApiResponse.<List<PhoneClickResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/my-clicks")
    @Operation(
            summary = "Get my phone click history",
            description = """
                    Get all listings the authenticated user has clicked phone numbers on.
                    
                    **Use Case:**
                    - User views their browsing history
                    - See which listings they showed interest in
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user's phone clicks",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    public ApiResponse<List<PhoneClickResponse>> getMyPhoneClicks() {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting phone clicks for user {}", userId);

        List<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksByUser(userId);

        return ApiResponse.<List<PhoneClickResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/listing/{listingId}/stats")
    @Operation(
            summary = "Get phone click statistics for a listing",
            description = """
                    Get statistics about phone clicks for a specific listing.
                    Includes total clicks and unique users count.
                    
                    **Use Case:**
                    - Renter views listing analytics
                    - Track engagement metrics
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved statistics",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found"
            )
    })
    public ApiResponse<PhoneClickStatsResponse> getPhoneClickStats(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId
    ) {
        log.info("Getting phone click stats for listing {}", listingId);

        PhoneClickStatsResponse response = phoneClickDetailService.getPhoneClickStats(listingId);

        return ApiResponse.<PhoneClickStatsResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    @GetMapping("/my-listings")
    @Operation(
            summary = "Get phone clicks for my listings",
            description = """
                    Get all phone clicks for listings owned by the authenticated user.
                    This is used in the renter's listing management page to see who is interested.
                    
                    **Use Case:**
                    - Renter views listing management dashboard
                    - See all users who clicked on any of their listings
                    - Contact interested users
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved phone clicks for owner's listings",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    public ApiResponse<List<PhoneClickResponse>> getPhoneClicksForMyListings() {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting phone clicks for all listings owned by user {}", userId);

        List<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksForOwnerListings(userId);

        return ApiResponse.<List<PhoneClickResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    /**
     * Extract IP address from HTTP request, handling proxy headers
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }
}

