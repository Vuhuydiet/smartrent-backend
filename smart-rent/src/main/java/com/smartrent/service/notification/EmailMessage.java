package com.smartrent.service.notification;

import com.smartrent.infra.connector.model.EmailRequest;
import java.util.List;

/**
 * Jackson round-trippable payload for an {@link NotificationType#EMAIL} job.
 * Kept separate from {@code EmailRequest} (which is immutable and not
 * deserializable) so jobs can be serialized onto and read back off the stream.
 */
public record EmailMessage(
    String senderEmail,
    String senderName,
    List<Recipient> recipients,
    String subject,
    String htmlContent) {

  public record Recipient(String name, String email) {
  }

  /** Maps the immutable {@code EmailRequest} into a serializable message. */
  public static EmailMessage from(EmailRequest request) {
    List<Recipient> recipients = request.getTo().stream()
        .map(info -> new Recipient(info.getName(), info.getEmail()))
        .toList();
    return new EmailMessage(
        request.getSender().getEmail(),
        request.getSender().getName(),
        recipients,
        request.getSubject(),
        request.getHtmlContent());
  }
}
