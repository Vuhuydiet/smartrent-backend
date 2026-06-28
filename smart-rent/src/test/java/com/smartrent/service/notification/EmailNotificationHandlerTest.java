package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.service.email.EmailService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EmailNotificationHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void handle_parsesEmailPayload_andCallsEmailService() throws Exception {
    AtomicReference<EmailRequest> sent = new AtomicReference<>();
    EmailService fakeEmailService = req -> {
      sent.set(req);
      return null; // EmailResponse is unused by the async send path
    };
    EmailNotificationHandler handler =
        new EmailNotificationHandler(fakeEmailService, objectMapper);

    EmailMessage message = new EmailMessage(
        "from@smartrent.vn", "SmartRent",
        List.of(new EmailMessage.Recipient("Alice", "alice@example.com")),
        "Verify your account", "<p>code 123456</p>");
    NotificationJob job =
        new NotificationJob(NotificationType.EMAIL, objectMapper.writeValueAsString(message));

    handler.handle(job);

    EmailRequest result = sent.get();
    assertThat(result).isNotNull();
    assertThat(result.getSubject()).isEqualTo("Verify your account");
    assertThat(result.getHtmlContent()).isEqualTo("<p>code 123456</p>");
    assertThat(result.getSender().getEmail()).isEqualTo("from@smartrent.vn");
    assertThat(result.getSender().getName()).isEqualTo("SmartRent");
    assertThat(result.getTo()).hasSize(1);
    assertThat(result.getTo().get(0).getEmail()).isEqualTo("alice@example.com");
    assertThat(result.getTo().get(0).getName()).isEqualTo("Alice");
  }

  @Test
  void supportedType_isEmail() {
    EmailNotificationHandler handler = new EmailNotificationHandler(null, objectMapper);
    assertThat(handler.supportedType()).isEqualTo(NotificationType.EMAIL);
  }
}
