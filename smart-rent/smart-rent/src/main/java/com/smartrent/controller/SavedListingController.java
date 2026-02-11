package com.smartrent.controller;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.SavedListingResponse;
import com.smartrent.service.listing.SavedListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/saved-listings")
@Tag(
        name = "Saved Listings",
        description = """
                API endpoints for managing user's saved/favorite listings.
                
                Users can:
                - Save (favorite) a listing to their personal collection
                - Remove a listing from their saved collection
                - View all their saved listings with full details (paginated)
                - Check if a specific listing is already saved
                - Get the total count of saved listings
                
                All endpoints require authentication.
                """
)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SavedListingController {

    SavedListingService savedListingService;

    @PostMapping
    @Operation(
            summary = "Save a listing",
            description = "Add a listing to the user's saved/favorite listings. User must be authenticated.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Listing ID to save",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SavedListingRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Save Listing Request",
                                    summary = "Example request to save a listing",
                                    description = "Provide the ID of the listing you want to save/favorite",
                                    value = """
                                            {
                                              "listingId": 123
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "userId": "550e8400-e29b-41d4-a716-446655440000",
                                                "listingId": 123,
                                                "createdAt": "2024-12-06T10:30:00",
                                                "updatedAt": "2024-12-06T10:30:00",
                                                "listing": null
                                              },
                                              "note": "The 'listing' field is null when saving a listing. Full listing details are returned when retrieving saved listings via GET /my-saved"
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
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "LISTING_NOT_FOUND",
                                              "message": "Listing not found with id: 123"
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
                                              "message": "Listing ID is required"
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Listing already saved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "LISTING_ALREADY_SAVED",
                                              "message": "Listing 123 is already saved by this user"
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<SavedListingResponse> saveListing(
            @Valid @RequestBody SavedListingRequest request) {
        SavedListingResponse response = savedListingService.saveListing(request);
        return ApiResponse.<SavedListingResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping("/{listingId}")
    @Operation(
            summary = "Remove a saved listing",
            description = """
                    Remove a listing from the user's saved/favorite listings.
                    
                    This endpoint is idempotent - calling it multiple times will have the same effect.
                    If the listing is not in the saved collection, a 404 error will be returned.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Listing removed from saved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Saved listing not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = """
                                            {
                                              "code": "SAVED_LISTING_NOT_FOUND",
                                              "message": "Saved listing not found for user and listing ID: 123"
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<Void> unsaveListing(
            @Parameter(
                    description = "ID of the listing to remove from saved collection",
                    required = true,
                    example = "123",
                    schema = @Schema(type = "integer", format = "int64")
            )
            @PathVariable Long listingId) {
        savedListingService.unsaveListing(listingId);
        return ApiResponse.<Void>builder()
                .build();
    }

    @GetMapping("/my-saved")
    @Operation(
            summary = "Get my saved listings",
            description = """
                    Retrieve all listings saved by the current user with full listing details (paginated).
                    
                    Each saved listing includes:
                    - Save timestamp (when the user saved it)
                    - Complete listing information (title, price, location, images, etc.)
                    - Owner/landlord contact information
                    - Listing status (verified, expired, etc.)
                    
                    Results are paginated for better performance.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Saved listings retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response with Listing Details",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": {
                                                "page": 1,
                                                "size": 10,
                                                "totalElements": 15,
                                                "totalPages": 2,
                                                "data": [
                                                  {
                                                    "userId": "550e8400-e29b-41d4-a716-446655440000",
                                                    "listingId": 123,
                                                      "createdAt": "2024-12-06T10:30:00",
                                                      "updatedAt": "2024-12-06T10:30:00",
                                                      "listing": {
                                                        "listingId": 123,
                                                        "title": "Cho thuê căn hộ 2PN Q7 view đẹp",
                                                        "description": "Căn hộ mới, full nội thất, gần trường học",
                                                        "user": {
                                                          "userId": "owner-123",
                                                          "username": "john_doe",
                                                          "email": "john@example.com",
                                                          "firstName": "John",
                                                          "lastName": "Doe",
                                                          "phoneNumber": "0901234567"
                                                        },
                                                        "ownerContactPhoneNumber": "0901234567",
                                                        "ownerContactPhoneVerified": true,
                                                        "ownerZaloLink": "https://zalo.me/0901234567",
                                                        "contactAvailable": true,
                                                        "postDate": "2024-12-01T08:00:00",
                                                        "expiryDate": "2024-12-31T23:59:59",
                                                        "listingType": "RENT",
                                                        "verified": true,
                                                        "isVerify": true,
                                                        "expired": false,
                                                        "isDraft": false,
                                                        "listingStatus": "DISPLAYING",
                                                        "vipType": "GOLD",
                                                        "categoryId": 1,
                                                        "productType": "APARTMENT",
                                                        "price": 15000000,
                                                        "priceUnit": "MONTH",
                                                        "address": {
                                                          "addressId": 456,
                                                          "street": "123 Nguyễn Văn Linh",
                                                          "legacy": {
                                                            "wardName": "Phường Tân Phú",
                                                            "districtName": "Quận 7",
                                                            "provinceName": "Hồ Chí Minh"
                                                          }
                                                        },
                                                        "area": 75.5,
                                                        "bedrooms": 2,
                                                        "bathrooms": 2,
                                                        "direction": "EAST",
                                                        "furnishing": "FULLY_FURNISHED",
                                                        "roomCapacity": 4,
                                                        "waterPrice": "20000 VND/người/tháng",
                                                        "electricityPrice": "3500 VND/kWh",
                                                        "internetPrice": "Miễn phí",
                                                        "serviceFee": "100000 VND/tháng",
                                                        "amenities": [
                                                          {
                                                            "amenityId": 1,
                                                            "name": "Wifi",
                                                            "icon": "wifi"
                                                          },
                                                          {
                                                            "amenityId": 2,
                                                            "name": "Điều hòa",
                                                            "icon": "ac"
                                                          }
                                                        ],
                                                        "media": [
                                                          {
                                                            "mediaId": 1,
                                                            "url": "https://example.com/image1.jpg",
                                                            "mediaType": "IMAGE",
                                                            "isPrimary": true,
                                                            "sortOrder": 0
                                                          }
                                                        ],
                                                        "createdAt": "2024-12-01T08:00:00",
                                                        "updatedAt": "2024-12-01T08:00:00"
                                                      }
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<PageResponse<SavedListingResponse>> getMySavedListings(
            @Parameter(
                    description = "Page number (1-indexed). First page is 1, not 0.",
                    example = "1",
                    schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")
            )
            @RequestParam(defaultValue = "1") int page,
            @Parameter(
                    description = "Number of items per page. Maximum value is 100.",
                    example = "10",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "10")
            )
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<SavedListingResponse> responses = savedListingService.getMySavedListings(page, size);
        return ApiResponse.<PageResponse<SavedListingResponse>>builder()
                .data(responses)
                .build();
    }

    @GetMapping("/check/{listingId}")
    @Operation(
            summary = "Check if listing is saved",
            description = """
                    Check whether a specific listing is in the user's saved/favorite collection.
                    
                    Returns true if the listing is saved, false otherwise.
                    This is useful for UI elements like "heart" icons to show save status.
                    
                    This endpoint does not return an error if the listing doesn't exist in the system,
                    it will simply return false.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Check completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Listing is saved",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": null,
                                                      "data": true
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "Listing is not saved",
                                            value = """
                                                    {
                                                      "code": "999999",
                                                      "message": null,
                                                      "data": false
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    public ApiResponse<Boolean> isListingSaved(
            @Parameter(
                    description = "ID of the listing to check if it's saved",
                    required = true,
                    example = "123",
                    schema = @Schema(type = "integer", format = "int64")
            )
            @PathVariable Long listingId) {
        boolean isSaved = savedListingService.isListingSaved(listingId);
        return ApiResponse.<Boolean>builder()
                .data(isSaved)
                .build();
    }

    @GetMapping("/count")
    @Operation(
            summary = "Get saved listings count",
            description = """
                    Get the total number of listings saved by the current user.
                    
                    This is a lightweight endpoint that returns only the count without fetching the actual listings.
                    Useful for displaying stats like "You have 15 saved listings" in the UI.
                    
                    Returns 0 if the user has no saved listings.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Count retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "code": "999999",
                                              "message": null,
                                              "data": 15
                                            }
                                            """
                            )
                    )
            )
    })
    public ApiResponse<Long> getMySavedListingsCount() {
        long count = savedListingService.getMySavedListingsCount();
        return ApiResponse.<Long>builder()
                .data(count)
                .build();
    }
}
