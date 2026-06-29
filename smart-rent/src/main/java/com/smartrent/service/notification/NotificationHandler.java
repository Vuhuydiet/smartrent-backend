package com.smartrent.service.notification;

/**
 * Performs the actual send for one {@link NotificationType}. Implementations run
 * on the consumer (worker) thread, off the request path.
 */
public interface NotificationHandler {
  NotificationType supportedType();

  void handle(NotificationJob job);
}
