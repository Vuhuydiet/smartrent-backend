package com.smartrent.service.notification;

/**
 * Producer side of the async notification pipeline. Request threads enqueue a
 * job and return immediately; a background consumer performs the blocking send.
 */
public interface NotificationQueue {
  void enqueue(NotificationJob job);
}
