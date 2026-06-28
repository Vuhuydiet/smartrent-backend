package com.smartrent.service.notification;

/**
 * A unit of work placed on the notification queue. {@code payload} is the
 * JSON body whose shape depends on {@link #type()} (e.g. an EmailMessage for
 * {@link NotificationType#EMAIL}).
 */
public record NotificationJob(NotificationType type, String payload) {
}
