package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.FollowStatusResponse;
import com.smartrent.dto.response.FollowedUserResponse;
import com.smartrent.service.follow.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User Follow", description = "Follow/unfollow other users and read follower lists")
public class UserFollowController {

    UserFollowService userFollowService;

    @PostMapping("/{userId}/follow")
    @Operation(
            summary = "Follow a user",
            description = "Idempotent. Following a user you already follow returns the current state.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ApiResponse<FollowStatusResponse> follow(@PathVariable("userId") String userId) {
        String currentUserId = currentUserId();
        FollowStatusResponse status = userFollowService.follow(currentUserId, userId);
        return ApiResponse.<FollowStatusResponse>builder().data(status).build();
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(
            summary = "Unfollow a user",
            description = "Idempotent. Unfollowing a user you do not follow returns the current state.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ApiResponse<FollowStatusResponse> unfollow(@PathVariable("userId") String userId) {
        String currentUserId = currentUserId();
        FollowStatusResponse status = userFollowService.unfollow(currentUserId, userId);
        return ApiResponse.<FollowStatusResponse>builder().data(status).build();
    }

    @GetMapping("/{userId}/follow-status")
    @Operation(
            summary = "Get follow status for a user",
            description = "Public. When called anonymously, isFollowing is always false; followerCount is always populated.")
    public ApiResponse<FollowStatusResponse> getStatus(@PathVariable("userId") String userId) {
        String currentUserId = currentUserIdOrNull();
        FollowStatusResponse status = userFollowService.getStatus(currentUserId, userId);
        return ApiResponse.<FollowStatusResponse>builder().data(status).build();
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "List users following the given user (paginated, newest first)")
    public ApiResponse<Page<FollowedUserResponse>> getFollowers(
            @PathVariable("userId") String userId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<FollowedUserResponse> result = userFollowService.getFollowers(currentUserIdOrNull(), userId, pageable);
        return ApiResponse.<Page<FollowedUserResponse>>builder().data(result).build();
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "List users the given user is following (paginated, newest first)")
    public ApiResponse<Page<FollowedUserResponse>> getFollowing(
            @PathVariable("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<FollowedUserResponse> result = userFollowService.getFollowing(currentUserIdOrNull(), userId, pageable);
        return ApiResponse.<Page<FollowedUserResponse>>builder().data(result).build();
    }

    private String currentUserId() {
        String id = currentUserIdOrNull();
        if (id == null || id.isBlank()) {
            // Endpoints requiring auth are protected by SecurityFilterChain; this is a defensive guard.
            throw new com.smartrent.infra.exception.AppException(
                    com.smartrent.infra.exception.model.DomainCode.UNAUTHENTICATED);
        }
        return id;
    }

    private String currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }
}
