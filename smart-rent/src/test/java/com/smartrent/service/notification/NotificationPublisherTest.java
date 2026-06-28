package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.enums.OtpChannel;
import com.smartrent.infra.connector.model.EmailInfo;
import com.smartrent.infra.connector.model.EmailRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPublisherTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void publishEmail_enqueuesEmailJobWithSerializedMessage() throws Exception {
    List<NotificationJob> enqueued = new ArrayList<>();
    NotificationQueue queue = enqueued::add;
    NotificationPublisher publisher = new NotificationPublisher(queue, objectMapper);

    EmailRequest request = EmailRequest.builder()
        .sender(EmailInfo.builder().email("from@smartrent.vn").name("SmartRent").build())
        .to(List.of(EmailInfo.builder().name("Alice").email("alice@example.com").build()))
        .subject("Verify your account")
        .htmlContent("<p>code 123456</p>")
        .build();

    publisher.publishEmail(request);

    assertThat(enqueued).hasSize(1);
    NotificationJob job = enqueued.get(0);
    assertThat(job.type()).isEqualTo(NotificationType.EMAIL);

    EmailMessage message = objectMapper.readValue(job.payload(), EmailMessage.class);
    assertThat(message.senderEmail()).isEqualTo("from@smartrent.vn");
    assertThat(message.subject()).isEqualTo("Verify your account");
    assertThat(message.htmlContent()).isEqualTo("<p>code 123456</p>");
    assertThat(message.recipients()).hasSize(1);
    assertThat(message.recipients().get(0).email()).isEqualTo("alice@example.com");
    assertThat(message.recipients().get(0).name()).isEqualTo("Alice");
  }

  @Test
  void publishOtp_enqueuesOtpJobWithSerializedMessage() throws Exception {
    List<NotificationJob> enqueued = new ArrayList<>();
    NotificationQueue queue = enqueued::add;
    NotificationPublisher publisher = new NotificationPublisher(queue, objectMapper);

    OtpMessage message = new OtpMessage(
        "+84900000000", "123456", List.of(OtpChannel.ZALO, OtpChannel.SMS), "req-1", 5);

    publisher.publishOtp(message);

    assertThat(enqueued).hasSize(1);
    NotificationJob job = enqueued.get(0);
    assertThat(job.type()).isEqualTo(NotificationType.OTP);

    OtpMessage parsed = objectMapper.readValue(job.payload(), OtpMessage.class);
    assertThat(parsed.phone()).isEqualTo("+84900000000");
    assertThat(parsed.otpCode()).isEqualTo("123456");
    assertThat(parsed.channelOrder()).containsExactly(OtpChannel.ZALO, OtpChannel.SMS);
    assertThat(parsed.requestId()).isEqualTo("req-1");
    assertThat(parsed.expiryMinutes()).isEqualTo(5);
  }
}
