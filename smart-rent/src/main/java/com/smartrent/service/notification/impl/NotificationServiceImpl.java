package com.smartrent.service.notification.impl;

import com.smartrent.dto.response.NotificationResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.NotificationRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Notification;
import com.smartrent.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AdminRepository adminRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void sendNotification(String recipientId, RecipientType recipientType,
                                 NotificationType type, String title, String message,
                                 Long referenceId, String referenceType) {
        try {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .recipientType(recipientType)
                    .type(type)
                    .title(title)
                    .message(message)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();

            Notification saved = notificationRepository.save(notification);
            log.info("Notification saved: type={}, recipient={}:{}", type, recipientType, recipientId);

            // Send realtime via WebSocket
            NotificationResponse response = mapToResponse(saved);
            String destination = "/topic/notifications/" + recipientId;
            messagingTemplate.convertAndSend(destination, response);
            log.debug("WebSocket notification sent to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send notification: type={}, recipient={}:{}, error={}",
                    type, recipientType, recipientId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendToAllAdmins(NotificationType type, String title, String message,
                                Long referenceId, String referenceType) {
        try {
            List<Admin> admins = adminRepository.findAll();
            for (Admin admin : admins) {
                sendNotification(admin.getAdminId(), RecipientType.ADMIN,
                        type, title, message, referenceId, referenceType);
            }
            log.info("Notification sent to {} admins: type={}", admins.size(), type);
        } catch (Exception e) {
            log.error("Failed to send notification to all admins: type={}, error={}", type, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String recipientId, RecipientType recipientType, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientId, RecipientType recipientType) {
        return notificationRepository.countByRecipientIdAndRecipientTypeAndIsReadFalse(recipientId, recipientType);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new DomainException(DomainCode.RESOURCE_NOT_FOUND, "Notification"));

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new DomainException(DomainCode.UNAUTHORIZED);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String recipientId, RecipientType recipientType) {
        int updated = notificationRepository.markAllAsRead(recipientId, recipientType);
        log.info("Marked {} notifications as read for {}:{}", updated, recipientType, recipientId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
