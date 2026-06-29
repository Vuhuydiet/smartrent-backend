package com.smartrent.service.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.enums.OtpChannel;
import com.smartrent.service.otp.provider.OtpProvider;
import com.smartrent.service.otp.provider.OtpProviderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OtpNotificationHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static OtpProvider provider(
      OtpChannel channel, boolean available, OtpProviderResult result, List<OtpChannel> calls) {
    return new OtpProvider() {
      @Override
      public OtpChannel getChannel() {
        return channel;
      }

      @Override
      public OtpProviderResult sendOtp(String phone, String otpCode, Map<String, Object> context) {
        calls.add(channel);
        return result;
      }

      @Override
      public boolean isAvailable() {
        return available;
      }

      @Override
      public String getProviderName() {
        return channel.name();
      }
    };
  }

  private NotificationJob otpJob(OtpMessage message) throws Exception {
    return new NotificationJob(NotificationType.OTP, objectMapper.writeValueAsString(message));
  }

  @Test
  void handle_sendsViaFirstChannel_andDoesNotFallBackOnSuccess() throws Exception {
    List<OtpChannel> calls = new ArrayList<>();
    OtpProvider zalo = provider(OtpChannel.ZALO, true, OtpProviderResult.success("msg-1"), calls);
    OtpProvider sms = provider(OtpChannel.SMS, true, OtpProviderResult.success("msg-2"), calls);
    OtpNotificationHandler handler = new OtpNotificationHandler(List.of(zalo, sms), objectMapper);

    handler.handle(otpJob(new OtpMessage(
        "+84900000000", "123456", List.of(OtpChannel.ZALO, OtpChannel.SMS), "req-1", 5)));

    assertThat(calls).containsExactly(OtpChannel.ZALO);
  }

  @Test
  void handle_fallsBackToNextChannel_whenFirstFails() throws Exception {
    List<OtpChannel> calls = new ArrayList<>();
    OtpProvider zalo =
        provider(OtpChannel.ZALO, true, OtpProviderResult.failure("E", "down", true), calls);
    OtpProvider sms = provider(OtpChannel.SMS, true, OtpProviderResult.success("msg-2"), calls);
    OtpNotificationHandler handler = new OtpNotificationHandler(List.of(zalo, sms), objectMapper);

    handler.handle(otpJob(new OtpMessage(
        "+84900000000", "123456", List.of(OtpChannel.ZALO, OtpChannel.SMS), "req-1", 5)));

    assertThat(calls).containsExactly(OtpChannel.ZALO, OtpChannel.SMS);
  }

  @Test
  void handle_skipsUnavailableProvider() throws Exception {
    List<OtpChannel> calls = new ArrayList<>();
    OtpProvider zalo = provider(OtpChannel.ZALO, false, OtpProviderResult.success("x"), calls);
    OtpProvider sms = provider(OtpChannel.SMS, true, OtpProviderResult.success("msg-2"), calls);
    OtpNotificationHandler handler = new OtpNotificationHandler(List.of(zalo, sms), objectMapper);

    handler.handle(otpJob(new OtpMessage(
        "+84900000000", "123456", List.of(OtpChannel.ZALO, OtpChannel.SMS), "req-1", 5)));

    assertThat(calls).containsExactly(OtpChannel.SMS);
  }

  @Test
  void supportedType_isOtp() {
    OtpNotificationHandler handler = new OtpNotificationHandler(List.of(), objectMapper);
    assertThat(handler.supportedType()).isEqualTo(NotificationType.OTP);
  }
}
