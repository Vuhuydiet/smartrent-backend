package com.smartrent.controller;

import com.smartrent.dto.request.RecentlyViewedSyncRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RecentlyViewedItemResponse;
import com.smartrent.service.recentlyviewed.RecentlyViewedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/recently-viewed")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Recently Viewed", description = "Endpoints for user browsing history")
public class RecentlyViewedController {

    RecentlyViewedService recentlyViewedService;

    @PostMapping("/sync")
    @Operation(summary = "Sync recently viewed listings", description = "Sync local browsing history with server. (Test mode: Accepts optional userId query param)")
    public ResponseEntity<ApiResponse<List<RecentlyViewedItemResponse>>> sync(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String userId,
            @Valid @RequestBody RecentlyViewedSyncRequest request) {
        
        String finalUserId = (jwt != null) ? jwt.getSubject() : userId;

        if (finalUserId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                    .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                    .message("User ID must be provided")
                    .build());
        }

        List<RecentlyViewedItemResponse> updatedHistory = recentlyViewedService.sync(finalUserId, request);

        return ResponseEntity.ok(ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Sync successful")
                .data(updatedHistory)
                .build());
    }

    @GetMapping
    @Operation(summary = "Get recently viewed listings", description = "Retrieve user's latest browsing history. (Test mode: Accepts optional userId query param)")
    public ResponseEntity<ApiResponse<List<RecentlyViewedItemResponse>>> getRecentlyViewed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String userId) {
        
        String finalUserId = (jwt != null) ? jwt.getSubject() : userId;

        if (finalUserId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                    .code(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                    .message("User ID must be provided")
                    .build());
        }

        List<RecentlyViewedItemResponse> history = recentlyViewedService.get(finalUserId);

        return ResponseEntity.ok(ApiResponse.<List<RecentlyViewedItemResponse>>builder()
                .code(String.valueOf(HttpStatus.OK.value()))
                .message("Successfully retrieved history")
                .data(history)
                .build());
    }


}
