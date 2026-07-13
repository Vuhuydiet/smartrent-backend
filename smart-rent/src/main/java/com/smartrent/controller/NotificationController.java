package com.smartrent.controller;

import com.smartrent.dto.response.NotificationResponse;
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
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(notificationService.getNotifications(recipientId, recipientType, pageable));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        String recipientId = JwtRecipientResolver.resolveRecipientId(jwt);
        RecipientType recipientType = JwtRecipientResolver.resolveRecipientType(jwt);

        long count = notificationService.getUnreadCount(recipientId, recipientType);
        return ResponseEntity.ok(Map.of("unreadCount", count));
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
