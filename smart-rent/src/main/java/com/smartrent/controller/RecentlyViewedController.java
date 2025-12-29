package com.smartrent.controller;

import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
import com.smartrent.service.recentlyviewed.RecentlyViewedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/recently-viewed")
@Tag(
        name = "Recently Viewed",
        description = """
                API endpoints for managing user's recently viewed listings.

                Features:
                - Sync recently viewed listings from client (localStorage) with server (Redis)
                - Get recently viewed listings sorted by most recent
                - Stores up to 20 most recent listings per user
                - Merges client and server data, keeping newest timestamps

                All endpoints require authentication.
                """
)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecentlyViewedController {

    RecentlyViewedService recentlyViewedService;

    @PostMapping("/sync")
    @Operation(
            summary = "Sync recently viewed listings",
            description = """
                    Sync recently viewed listings from client localStorage with server Redis storage.

                    This endpoint:
                    - Accepts a list of recently viewed listings from the client
                    - Merges them with existing server data
                    - For duplicate listing IDs, keeps the newest timestamp
                    - Trims the list to keep only the 20 most recent listings
                    - Returns the merged, sorted list

                    The client should call this endpoint after login to sync localStorage data.
                    Empty client payloads are accepted (useful for first-time sync).

                    Timestamps are validated - negative or far future timestamps are ignored.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Recently viewed listings from client",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RecentlyViewedSyncRequest.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "With listings",
                                            summary = "Sync request with listings",
                                            description = "Normal sync with recently viewed listings from localStorage",
                                            value = """
                                                    {
                                                      "listings": [
                                                        {
                                                          "listingId": 123,
                                                          "viewedAt": 1703592000000
                                                        },
                                                        {
                                                          "listingId": 456,
                                                          "viewedAt": 1703595600000
                                                        },
                                                        {
                                                          "listingId": 789,
                                                          "viewedAt": 1703599200000
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Empty listings",
                                            summary = "First-time sync",
                                            description = "Sync with empty client data (first-time user or cleared localStorage)",
                                            value = """
                                                    {
                                                      "listings": []
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Sync completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": [
                                                {
                                                  "listingId": 789,
                                                  "viewedAt": 1703599200000
                                                },
                                                {
                                                  "listingId": 456,
                                                  "viewedAt": 1703595600000
                                                },
                                                {
                                                  "listingId": 123,
                                                  "viewedAt": 1703592000000
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Bad request - Invalid input",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                              "code": "BAD_REQUEST_ERROR",
                                              "message": "Listings list is required (can be empty)"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "5001",
                                              "message": "Unauthenticated"
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<RecentlyViewedItemResponse>> syncRecentlyViewed(
            @Valid @RequestBody RecentlyViewedSyncRequest request) {
        List<RecentlyViewedItemResponse> response = recentlyViewedService.syncRecentlyViewed(request);
        return ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get recently viewed listings",
            description = """
                    Retrieve the user's recently viewed listings from Redis storage.

                    Returns up to 20 most recently viewed listings, sorted by view timestamp (most recent first).

                    This endpoint:
                    - Retrieves data from Redis (fast, O(log N) operation)
                    - Returns listings sorted by most recent
                    - Returns empty list if no listings exist (first-time user)
                    - Does not perform any database queries

                    The client can call this endpoint after login to get the initial list,
                    or periodically to check for updates.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "With listings",
                                            summary = "User has recently viewed listings",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": null,
                                                      "data": [
                                                        {
                                                          "listingId": 789,
                                                          "viewedAt": 1703599200000
                                                        },
                                                        {
                                                          "listingId": 456,
                                                          "viewedAt": 1703595600000
                                                        },
                                                        {
                                                          "listingId": 123,
                                                          "viewedAt": 1703592000000
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "No listings",
                                            summary = "First-time user or no recent views",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": null,
                                                      "data": []
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "5001",
                                              "message": "Unauthenticated"
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<List<RecentlyViewedItemResponse>> getRecentlyViewed() {
        List<RecentlyViewedItemResponse> response = recentlyViewedService.getRecentlyViewed();
        return ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                .data(response)
                .build();
    }
}