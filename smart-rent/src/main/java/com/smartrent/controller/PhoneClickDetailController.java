package com.smartrent.controller;

import com.smartrent.dto.request.PhoneClickRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PhoneClickResponse;
import com.smartrent.dto.response.PhoneClickStatsResponse;
import com.smartrent.dto.response.UserPhoneClickDetailResponse;
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
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "id": 1,
                                                "listingId": 123,
                                                "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                "userFirstName": "John",
                                                "userLastName": "Doe",
                                                "userEmail": "john.doe@example.com",
                                                "userContactPhone": "0912345678",
                                                "userContactPhoneVerified": true,
                                                "userAvatarUrl": "https://lh3.googleusercontent.com/a/example",
                                                "clickedAt": "2024-01-15T10:30:00",
                                                "ipAddress": "192.168.1.1"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
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
                    Get all users who clicked on a specific listing's phone number (paginated).
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
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 25,
                                                "totalPages": 3,
                                                "data": [
                                                  {
                                                    "id": 1,
                                                    "listingId": 123,
                                                    "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "userFirstName": "John",
                                                    "userLastName": "Doe",
                                                    "userEmail": "john.doe@example.com",
                                                    "userContactPhone": "0912345678",
                                                    "userContactPhoneVerified": true,
                                                    "userAvatarUrl": "https://lh3.googleusercontent.com/a/example",
                                                    "clickedAt": "2024-01-15T10:30:00",
                                                    "ipAddress": "192.168.1.1"
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
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<PhoneClickResponse>> getPhoneClicksByListing(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Getting phone clicks for listing {} - page: {}, size: {}", listingId, page, size);

        PageResponse<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksByListing(listingId, page, size);

        return ApiResponse.<PageResponse<PhoneClickResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/listing/{listingId}/users")
    @Operation(
            summary = "Get user details who clicked on listing's phone number",
            description = """
                    Get all users who clicked on a specific listing's phone number (paginated).
                    Each user detail contains a list of listings they have clicked on phone numbers.

                    **Use Case:**
                    - Renter views listing management page
                    - See which users are interested in the listing with their click history
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user details with clicked listings",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 5,
                                                "totalPages": 1,
                                                "data": [
                                                  {
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "firstName": "John",
                                                    "lastName": "Doe",
                                                    "email": "john.doe@example.com",
                                                    "contactPhone": "0912345678",
                                                    "contactPhoneVerified": true,
                                                    "avatarUrl": "https://example.com/avatar.jpg",
                                                    "totalListingsClicked": 2,
                                                    "clickedListings": [
                                                      {
                                                        "listingId": 123,
                                                        "listingTitle": "Beautiful 2BR Apartment",
                                                        "clickedAt": "2024-01-15T10:30:00",
                                                        "clickCount": 3
                                                      }
                                                    ]
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
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<UserPhoneClickDetailResponse>> getUsersWithClickedListings(
            @Parameter(description = "Listing ID", example = "123")
            @PathVariable Long listingId,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Getting users with clicked listings for listing {} - page: {}, size: {}", listingId, page, size);

        PageResponse<UserPhoneClickDetailResponse> responses = phoneClickDetailService.getUsersWithClickedListings(listingId, page, size);

        return ApiResponse.<PageResponse<UserPhoneClickDetailResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/my-clicks")
    @Operation(
            summary = "Get my phone click history",
            description = """
                    Get all listings the authenticated user has clicked phone numbers on (paginated).

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
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 15,
                                                "totalPages": 2,
                                                "data": [
                                                  {
                                                    "id": 1,
                                                    "listingId": 123,
                                                    "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "userFirstName": "John",
                                                    "userLastName": "Doe",
                                                    "userEmail": "john.doe@example.com",
                                                    "userContactPhone": "0912345678",
                                                    "userContactPhoneVerified": true,
                                                    "userAvatarUrl": "https://lh3.googleusercontent.com/a/example",
                                                    "clickedAt": "2024-01-15T10:30:00",
                                                    "ipAddress": "192.168.1.1"
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
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<PhoneClickResponse>> getMyPhoneClicks(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting phone clicks for user {} - page: {}, size: {}", userId, page, size);

        PageResponse<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksByUser(userId, page, size);

        return ApiResponse.<PageResponse<PhoneClickResponse>>builder()
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
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "listingId": 123,
                                                "totalClicks": 25,
                                                "uniqueUsers": 18
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = """
                                            {
                                              "code": "404001",
                                              "message": "Listing not found",
                                              "data": null
                                            }
                                            """
                            )
                    )
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
                    Get all phone clicks for listings owned by the authenticated user (paginated).
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
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 50,
                                                "totalPages": 5,
                                                "data": [
                                                  {
                                                    "id": 1,
                                                    "listingId": 123,
                                                    "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "userFirstName": "John",
                                                    "userLastName": "Doe",
                                                    "userEmail": "john.doe@example.com",
                                                    "userContactPhone": "0912345678",
                                                    "userContactPhoneVerified": true,
                                                    "userAvatarUrl": "https://lh3.googleusercontent.com/a/example",
                                                    "clickedAt": "2024-01-15T10:30:00",
                                                    "ipAddress": "192.168.1.1"
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
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<PhoneClickResponse>> getPhoneClicksForMyListings(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting phone clicks for all listings owned by user {} - page: {}, size: {}", userId, page, size);

        PageResponse<PhoneClickResponse> responses = phoneClickDetailService.getPhoneClicksForOwnerListings(userId, page, size);

        return ApiResponse.<PageResponse<PhoneClickResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/my-listings/users")
    @Operation(
            summary = "Get users who clicked on my listings",
            description = """
                    Get all users who clicked on phone numbers in any of the authenticated user's listings (paginated).
                    Each user detail contains a list of the owner's listings they have clicked on.

                    **Use Case:**
                    - Renter views listing management dashboard
                    - See all users who showed interest in any of their listings
                    - Click on a user to see which specific listings they were interested in
                    - Contact interested users
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved users who clicked on owner's listings",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 25,
                                                "totalPages": 3,
                                                "data": [
                                                  {
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "firstName": "John",
                                                    "lastName": "Doe",
                                                    "email": "john.doe@example.com",
                                                    "contactPhone": "0912345678",
                                                    "contactPhoneVerified": true,
                                                    "avatarUrl": null,
                                                    "totalListingsClicked": 3,
                                                    "clickedListings": [
                                                      {
                                                        "listingId": 123,
                                                        "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                        "clickedAt": "2024-01-15T10:30:00",
                                                        "clickCount": 2
                                                      },
                                                      {
                                                        "listingId": 456,
                                                        "listingTitle": "Cozy Studio near City Center",
                                                        "clickedAt": "2024-01-14T15:45:00",
                                                        "clickCount": 1
                                                      }
                                                    ]
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
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<UserPhoneClickDetailResponse>> getUsersWhoClickedOnMyListings(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Getting users who clicked on listings owned by user {} - page: {}, size: {}", userId, page, size);

        PageResponse<UserPhoneClickDetailResponse> responses = phoneClickDetailService.getUsersWhoClickedOnMyListings(userId, page, size);

        return ApiResponse.<PageResponse<UserPhoneClickDetailResponse>>builder()
                .code("999999")
                .data(responses)
                .build();
    }

    @GetMapping("/my-listings/search")
    @Operation(
            summary = "Search phone clicks for my listings by title",
            description = """
                    Search for users who clicked on phone numbers in listings owned by the authenticated user,
                    filtered by listing title keyword (paginated).

                    **Use Case:**
                    - Renter wants to find who clicked on a specific listing
                    - Search by listing title to narrow down results
                    - See interested users for specific properties

                    **Search Behavior:**
                    - Case-insensitive partial match on listing title
                    - Returns phone clicks with user details and listing title
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved search results",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": "Success",
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 8,
                                                "totalPages": 1,
                                                "data": [
                                                  {
                                                    "id": 1,
                                                    "listingId": 123,
                                                    "listingTitle": "Beautiful 2BR Apartment in District 1",
                                                    "userId": "user-123e4567-e89b-12d3-a456-426614174000",
                                                    "userFirstName": "John",
                                                    "userLastName": "Doe",
                                                    "userEmail": "john.doe@example.com",
                                                    "userContactPhone": "0912345678",
                                                    "userContactPhoneVerified": true,
                                                    "userAvatarUrl": "https://lh3.googleusercontent.com/a/example",
                                                    "clickedAt": "2024-01-15T10:30:00",
                                                    "ipAddress": "192.168.1.1"
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid parameters",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Bad Request Error",
                                    value = """
                                            {
                                              "code": "400001",
                                              "message": "Invalid parameters",
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = """
                                            {
                                              "code": "401001",
                                              "message": "Unauthorized - User not authenticated",
                                              "data": null
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<PhoneClickResponse>> searchPhoneClicksByListingTitle(
            @Parameter(description = "Listing title keyword to search for", example = "apartment", required = true)
            @RequestParam String title,
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        // Get authenticated user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Searching phone clicks for listings owned by user {} with title keyword '{}' - page: {}, size: {}",
                userId, title, page, size);

        PageResponse<PhoneClickResponse> responses = phoneClickDetailService.searchPhoneClicksByListingTitle(
                userId, title, page, size);

        return ApiResponse.<PageResponse<PhoneClickResponse>>builder()
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

