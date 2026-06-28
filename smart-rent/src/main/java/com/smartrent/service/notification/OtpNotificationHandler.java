package com.smartrent.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.enums.OtpChannel;
import com.smartrent.service.otp.provider.OtpProvider;
import com.smartrent.service.otp.provider.OtpProviderResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumes {@link NotificationType#OTP} jobs and runs the channel-fallback send
 * (e.g. Zalo -> SMS) on the worker thread, off the request path.
 */
@Slf4j
public class OtpNotificationHandler implements NotificationHandler {

  private final Map<OtpChannel, OtpProvider> providerMap;
  private final ObjectMapper objectMapper;

  public OtpNotificationHandler(List<OtpProvider> providers, ObjectMapper objectMapper) {
    this.providerMap = providers.stream()
        .collect(Collectors.toMap(OtpProvider::getChannel, Function.identity()));
    this.objectMapper = objectMapper;
  }

  @Override
  public NotificationType supportedType() {
    return NotificationType.OTP;
  }

  @Override
  public void handle(NotificationJob job) {
    OtpMessage message = parse(job);

    Map<String, Object> context = new HashMap<>();
    context.put("requestId", message.requestId());
    context.put("expiryMinutes", message.expiryMinutes());

    for (OtpChannel channel : message.channelOrder()) {
      OtpProvider provider = providerMap.get(channel);
      if (provider == null || !provider.isAvailable()) {
        log.warn("OTP provider unavailable for channel {} (requestId={})", channel,
            message.requestId());
        continue;
      }
      OtpProviderResult result = provider.sendOtp(message.phone(), message.otpCode(), context);
      if (result.isSuccess()) {
        log.info("OTP sent via {}: requestId={}, messageId={}", channel, message.requestId(),
            result.getMessageId());
        return;
      }
      log.warn("Failed to send OTP via {}: requestId={}, error={}", channel, message.requestId(),
          result.getErrorMessage());
    }
    log.error("Failed to send OTP via all channels: requestId={}", message.requestId());
  }

  private OtpMessage parse(NotificationJob job) {
    try {
      return objectMapper.readValue(job.payload(), OtpMessage.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid OTP notification payload", e);
    }
  }
}
