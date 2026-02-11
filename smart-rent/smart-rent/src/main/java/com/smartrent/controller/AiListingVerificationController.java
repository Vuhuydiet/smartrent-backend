package com.smartrent.controller;

import com.smartrent.dto.request.AiListingVerificationRequest;
import com.smartrent.dto.response.AiListingVerificationResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.service.ai.AiListingVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/ai/listings")
@Tag(name = "AI Listing Verification", description = "AI-powered listing verification and validation APIs")
@RequiredArgsConstructor
// @SecurityRequirement(name = "Bearer Authentication") // Temporarily disabled for testing
public class AiListingVerificationController {

    private final AiListingVerificationService aiListingVerificationService;

    @PostMapping("/verify")
    // @PreAuthorize("authentication.authorities.stream().anyMatch(a -> a.authority.startsWith('ROLE_ADMIN') || a.authority.startsWith('ROLE_SUPER_ADMIN') || a.authority.startsWith('ROLE_USER'))") // Temporarily disabled for testing
    @Operation(
        summary = "Verify listing using AI",
        description = "Analyzes a listing using AI to check for quality, completeness, and compliance issues. " +
                     "Returns detailed verification results with scores and suggestions for improvement. " +
                     "Note: metadata fields (furnished, pet_friendly, parking_available) are optional and will default to false if not provided.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Listing verification request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AiListingVerificationRequest.class),
                examples = @ExampleObject(
                    name = "Complete listing request",
                    value = """
                        {
                          "title": "Cho thuê căn hộ 3PN view sông Vinhomes Central Park",
                          "description": "Căn hộ 3 phòng ngủ cao cấp tại tòa nhà Vinhomes Central Park, view sông Sài Gòn tuyệt đẹp. Diện tích 95m2, đầy đủ nội thất cao cấp nhập khẩu.",
                          "price": 35000000,
                          "address": "208 Nguyễn Hữu Cảnh, Phường 22, Quận Bình Thạnh, TP.HCM",
                          "property_type": "APARTMENT",
                          "area": 95,
                          "amenities": ["WIFI", "AIR_CONDITIONING", "SWIMMING_POOL", "GYM", "PARKING"],
                          "images": [
                            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2",
                            "https://images.unsplash.com/photo-1586023492125-27b2c045efd7",
                            "https://images.unsplash.com/photo-1449824913935-59a10b8d2000"
                          ],
                          "videos": [
                            {"url": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"},
                            {"url": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"}
                          ],
                          "metadata": {
                            "bedrooms": 3,
                            "bathrooms": 2
                          }
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Verification completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Successful verification",
                    value = """
                        {
                          "code": "999999",
                          "message": "Listing verification completed successfully",
                          "data": {
                            "is_valid": false,
                            "score": 0.65,
                            "confidence": 0.9,
                            "image_validation": {
                              "is_valid": false,
                              "total_images": 1,
                              "valid_images": 1,
                              "issues": [
                                "Image quality is excellent, but view doesn't match described 'Saigon River view'",
                                "Only one image provided, insufficient for 2-bedroom apartment"
                              ],
                              "quality_score": 0.7
                            },
                            "content_validation": {
                              "is_rental_related": true,
                              "category_match": true,
                              "content_score": 0.9,
                              "issues": []
                            },
                            "completeness_validation": {
                              "is_complete": false,
                              "completeness_score": 0.8,
                              "missing_fields": [],
                              "quality_issues": [
                                "Price currency ambiguity: '$20000000.0/month' should be '20,000,000 VND/month'"
                              ]
                            },
                            "violations": [
                              {
                                "category": "pricing",
                                "severity": "medium",
                                "message": "Price currency ambiguity detected",
                                "field": "price"
                              }
                            ],
                            "suggestions": [
                              {
                                "category": "clarification",
                                "message": "Clearly specify currency as VND to avoid confusion",
                                "field": null,
                                "priority": "high"
                              },
                              {
                                "category": "images",
                                "message": "Add more diverse images of all apartment areas",
                                "field": null,
                                "priority": "high"
                              }
                            ],
                            "verification_timestamp": "2025-11-26T00:18:27.330088",
                            "model_used": "gemini-2.5-flash",
                            "processing_time_seconds": 18.97
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                        {
                          "code": "4001",
                          "message": "Validation failed",
                          "data": {
                            "title": "Title is required",
                            "description": "Description must be between 10 and 5000 characters"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "AI service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Service unavailable",
                    value = """
                        {
                          "code": "5003",
                          "message": "AI verification service is currently unavailable",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<AiListingVerificationResponse>> verifyListing(
            @Valid @RequestBody AiListingVerificationRequest request) {

        log.info("Received AI verification request for listing: {}", request.getTitle());

        AiListingVerificationResponse response = aiListingVerificationService.verifyListing(request);

        return ResponseEntity.ok(ApiResponse.<AiListingVerificationResponse>builder()
                .message("Listing verification completed successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{listingId}/verify")
    // @PreAuthorize("authentication.authorities.stream().anyMatch(a -> a.authority.startsWith('ROLE_ADMIN') || a.authority.startsWith('ROLE_SUPER_ADMIN'))") // Temporarily disabled for testing
    @Operation(
        summary = "Verify existing listing by ID",
        description = "Verifies an existing listing in the system using its ID. Only admins can perform this operation.",
        parameters = {
            @Parameter(
                name = "listingId",
                description = "The ID of the listing to verify",
                required = true,
                example = "123"
            )
        }
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Listing verified successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Listing not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Listing not found",
                    value = """
                        {
                          "code": "4003",
                          "message": "Listing not found",
                          "data": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Access denied - Admin role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public ResponseEntity<ApiResponse<AiListingVerificationResponse>> verifyListingById(
            @PathVariable Long listingId) {

        log.info("Received AI verification request for listing ID: {}", listingId);

        // Note: This would require injecting ListingService to fetch the listing
        // For now, we'll leave this as a placeholder for future implementation
        throw new UnsupportedOperationException("Verify by listing ID not yet implemented");
    }

    @GetMapping("/service-status")
    @Operation(
        summary = "Check AI verification service status",
        description = "Returns the availability status of the AI verification service"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Service status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Service available",
                    value = """
                        {
                          "code": "999999",
                          "message": "AI service status retrieved successfully",
                          "data": {
                            "available": true,
                            "checked_at": "2025-11-23T15:30:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Object>> getServiceStatus() {
        boolean isAvailable = aiListingVerificationService.isServiceAvailable();

        return ResponseEntity.ok(ApiResponse.builder()
                .message("AI service status retrieved successfully")
                .data(new Object() {
                    public final boolean available = isAvailable;
                    public final String checked_at = java.time.LocalDateTime.now().toString();
                })
                .build());
    }
}