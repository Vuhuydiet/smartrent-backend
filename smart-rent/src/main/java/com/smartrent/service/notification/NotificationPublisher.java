package com.smartrent.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.infra.connector.model.EmailRequest;
import org.springframework.stereotype.Service;

/**
 * Producer-facing API for callers that want to send a notification
 * asynchronously. Hides the job serialization and queue details.
 */
@Service
public class NotificationPublisher {

  private final NotificationQueue queue;
  private final ObjectMapper objectMapper;

  public NotificationPublisher(NotificationQueue queue, ObjectMapper objectMapper) {
    this.queue = queue;
    this.objectMapper = objectMapper;
  }

  /** Enqueue an email to be sent on a worker thread instead of the caller's. */
  public void publishEmail(EmailRequest request) {
    queue.enqueue(new NotificationJob(NotificationType.EMAIL, serialize(EmailMessage.from(request))));
  }

  /** Enqueue an OTP to be delivered (with channel fallback) on a worker thread. */
  public void publishOtp(OtpMessage message) {
    queue.enqueue(new NotificationJob(NotificationType.OTP, serialize(message)));
  }

  private String serialize(Object message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize notification payload", e);
    }
  }
}
