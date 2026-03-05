package com.smartrent.service.notification;

import com.smartrent.dto.response.NotificationResponse;
import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for creating, storing, and delivering in-app notifications.
 * Notifications are persisted to the database and pushed via WebSocket in realtime.
 */
public interface NotificationService {

    /**
     * Create and send a notification to a specific recipient.
     */
    void sendNotification(String recipientId, RecipientType recipientType,
                          NotificationType type, String title, String message,
                          Long referenceId, String referenceType);

    /**
     * Create and send a notification to ALL admins.
     */
    void sendToAllAdmins(NotificationType type, String title, String message,
                         Long referenceId, String referenceType);

    /**
     * Get paginated notifications for a recipient.
     */
    Page<NotificationResponse> getNotifications(String recipientId, RecipientType recipientType, Pageable pageable);

    /**
     * Get the count of unread notifications.
     */
    long getUnreadCount(String recipientId, RecipientType recipientType);

    /**
     * Mark a single notification as read.
     */
    void markAsRead(Long notificationId, String recipientId);

    /**
     * Mark all notifications as read for a recipient.
     */
    void markAllAsRead(String recipientId, RecipientType recipientType);
}
