package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.NotificationResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.enums.RecipientType;
import com.smartrent.service.notification.NotificationService;
import com.smartrent.util.JwtRecipientResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications",
            description = "Get paginated notification history for the authenticated user/admin")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationResponse> notifications =
                notificationService.getNotifications(recipientId, recipientType, pageable);

        PageResponse<NotificationResponse> pageResponse = PageResponse.<NotificationResponse>builder()
                .page(notifications.getNumber())
                .size(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .data(notifications.getContent())
                .build();

        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .data(pageResponse)
                .build();
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);

        long count = notificationService.getUnreadCount(recipientId, recipientType);
        return ApiResponse.<Map<String, Long>>builder()
                .data(Map.of("unreadCount", count))
                .build();
    }

    @Operation(summary = "Mark a notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);
        notificationService.markAsRead(id, recipientId, recipientType);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);

        notificationService.markAllAsRead(recipientId, recipientType);
        return ResponseEntity.ok().build();
    }
}
