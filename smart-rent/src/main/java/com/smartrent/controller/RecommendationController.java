package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RecommendationResponse;
import com.smartrent.service.recommendation.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Recommendations", description = "Endpoints for hybrid AI recommendations (similar & personalized feed)")
public class RecommendationController {

    RecommendationService recommendationService;

    /**
     * GET /v1/recommendations/similar/{listingId}
     *
     * Returns listings similar to the given listing.
     * If the caller is authenticated, results are slightly personalized
     * (60% content-based similarity, 40% user preference).
     * Anonymous access is allowed — userId is derived from JWT if present.
     */
    @GetMapping("/similar/{listingId}")
    @Operation(
            summary = "Get similar listings",
            description = """
                    Returns listings similar to the specified listing ID.
                    If authenticated, results are personalized (60% similar, 40% user preference).
                    Anonymous access is allowed.
                    """)
    public ResponseEntity<ApiResponse<RecommendationResponse>> getSimilarListings(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "8") int topN) {

        String userId = (jwt != null) ? jwt.getSubject() : null;
        RecommendationResponse response = recommendationService.getSimilarListings(listingId, topN, userId);

        return ResponseEntity.ok(ApiResponse.<RecommendationResponse>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Successfully retrieved similar listings")
                .data(response)
                .build());
    }

    /**
     * GET /v1/recommendations/personalized
     *
     * Returns a personalized listing feed based on the authenticated user's
     * browsing history, saved listings, and phone clicks (Hybrid CF + CBF).
     *
     * Test convenience: accepts optional {@code userId} query param when JWT
     * is not present (useful during development/Swagger testing).
     */
    @GetMapping("/personalized")
    @Operation(
            summary = "Get personalized feed",
            description = """
                    Returns personalized listings based on user browsing history, saved listings,
                    and phone clicks (Hybrid CF + CBF).
                    Requires authentication. In test mode, accepts an optional userId query param.
                    Falls back to cold-start (trending) if the user has no interaction history.
                    """)
    public ResponseEntity<ApiResponse<RecommendationResponse>> getPersonalizedFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "20") int topN) {

        String finalUserId = (jwt != null) ? jwt.getSubject() : userId;

        if (finalUserId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<RecommendationResponse>builder()
                            .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                            .message("User ID must be provided via authentication or userId parameter")
                            .build());
        }

        RecommendationResponse response = recommendationService.getPersonalizedFeed(finalUserId, topN);

        return ResponseEntity.ok(ApiResponse.<RecommendationResponse>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Successfully retrieved personalized feed")
                .data(response)
                .build());
    }
}
