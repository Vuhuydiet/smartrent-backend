package com.smartrent.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.service.email.EmailService;
import java.util.List;

/**
 * Consumes {@link NotificationType#EMAIL} jobs off the queue and performs the
 * actual (blocking) provider send on the worker thread instead of the request
 * thread.
 */
public class EmailNotificationHandler implements NotificationHandler {

  private final EmailService emailService;
  private final ObjectMapper objectMapper;

  public EmailNotificationHandler(EmailService emailService, ObjectMapper objectMapper) {
    this.emailService = emailService;
    this.objectMapper = objectMapper;
  }

  @Override
  public NotificationType supportedType() {
    return NotificationType.EMAIL;
  }

  @Override
  public void handle(NotificationJob job) {
    EmailMessage message;
    try {
      message = objectMapper.readValue(job.payload(), EmailMessage.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid email notification payload", e);
    }
    emailService.sendEmail(toEmailRequest(message));
  }

  private EmailRequest toEmailRequest(EmailMessage message) {
    List<EmailInfo> recipients = message.recipients().stream()
        .map(r -> EmailInfo.builder().name(r.name()).email(r.email()).build())
        .toList();
    return EmailRequest.builder()
        .sender(EmailInfo.builder().email(message.senderEmail()).name(message.senderName()).build())
        .to(recipients)
        .subject(message.subject())
        .htmlContent(message.htmlContent())
        .build();
  }
}
