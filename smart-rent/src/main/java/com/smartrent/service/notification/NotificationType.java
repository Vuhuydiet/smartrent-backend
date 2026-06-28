package com.smartrent.service.notification;

/**
 * Kind of asynchronous notification carried on the Redis Stream queue.
 */
public enum NotificationType {
  EMAIL,
  OTP
}
