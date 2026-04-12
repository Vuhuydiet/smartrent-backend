package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RecommendationResponse;
import com.smartrent.service.recommendation.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @Operation(summary = "Get similar listings", description = "Returns listings similar to the specified listing ID (Public)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getSimilarListings(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "8") int topN) {
        
        RecommendationResponse response = recommendationService.getSimilarListings(listingId, topN);

        return ResponseEntity.ok(ApiResponse.<RecommendationResponse>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Successfully retrieved similar listings")
                .data(response)
                .build());
    }

    @GetMapping("/personalized")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get personalized feed", description = "Returns personalized listings based on user browsing and saved history. Requires authentication.")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getPersonalizedFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int topN) {
        
        String userId = jwt.getSubject();
        RecommendationResponse response = recommendationService.getPersonalizedFeed(userId, topN);

        return ResponseEntity.ok(ApiResponse.<RecommendationResponse>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Successfully retrieved personalized feed")
                .data(response)
                .build());
    }


}
