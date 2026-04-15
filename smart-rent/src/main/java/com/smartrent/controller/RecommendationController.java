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
@Tag(name = "Recommendations", description = "Endpoints for hybrid AI recommendations")
public class RecommendationController {

    RecommendationService recommendationService;

    @GetMapping("/similar/{listingId}")
    @Operation(summary = "Get similar listings", description = "Returns listings similar to the specified listing ID. If authenticated, results are personalized (60% similar, 40% user preference).")
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

    @GetMapping("/personalized")
    @Operation(summary = "Get personalized feed", description = "Returns personalized listings based on user browsing and saved history. (Test mode: Accepts optional testUserId if not logged in)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getPersonalizedFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "20") int topN) {

        String finalUserId = (jwt != null) ? jwt.getSubject() : userId;

        if (finalUserId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<RecommendationResponse>builder()
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
