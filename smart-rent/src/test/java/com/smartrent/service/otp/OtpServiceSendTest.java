package com.smartrent.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartrent.config.otp.OtpProperties;
import com.smartrent.dto.request.OtpSendRequest;
import com.smartrent.dto.response.OtpSendResponse;
import com.smartrent.enums.OtpChannel;
import com.smartrent.infra.exception.OtpException;
import com.smartrent.service.notification.NotificationPublisher;
import com.smartrent.service.notification.OtpMessage;
import com.smartrent.service.otp.store.OtpData;
import com.smartrent.service.otp.store.OtpStore;
import com.smartrent.service.otp.util.OtpUtil;
import com.smartrent.service.otp.util.PhoneNumberUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpServiceSendTest {

  @Mock OtpProperties otpProperties;
  @Mock OtpStore otpStore;
  @Mock OtpUtil otpUtil;
  @Mock PhoneNumberUtil phoneNumberUtil;
  @Mock RateLimitService rateLimitService;
  @Mock NotificationPublisher notificationPublisher;

  private OtpService newService() {
    return new OtpService(otpProperties, otpStore, otpUtil, phoneNumberUtil,
        rateLimitService, notificationPublisher, new SimpleMeterRegistry());
  }

  @Test
  void sendOtp_storesOtpThenEnqueuesJob_andReturnsPrimaryChannel() {
    when(phoneNumberUtil.normalizeAndValidate("0900000000")).thenReturn("+84900000000");
    when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("+84*** masked");
    when(otpUtil.generateOtpCode()).thenReturn("123456");
    when(otpUtil.hashOtpCode("123456")).thenReturn("hashed");
    when(otpProperties.getTtlSeconds()).thenReturn(300);
    when(otpProperties.getMaxVerificationAttempts()).thenReturn(5);
    when(otpStore.store(any(OtpData.class), eq(300))).thenReturn(true);

    OtpSendRequest request = OtpSendRequest.builder().phone("0900000000").build();

    OtpSendResponse response = newService().sendOtp(request, "1.2.3.4");

    // OTP must be stored before the delivery job is enqueued.
    InOrder inOrder = inOrder(otpStore, notificationPublisher);
    inOrder.verify(otpStore).store(any(OtpData.class), eq(300));
    inOrder.verify(notificationPublisher).publishOtp(any(OtpMessage.class));

    ArgumentCaptor<OtpMessage> captor = ArgumentCaptor.forClass(OtpMessage.class);
    verify(notificationPublisher).publishOtp(captor.capture());
    OtpMessage message = captor.getValue();
    assertThat(message.phone()).isEqualTo("+84900000000");
    assertThat(message.otpCode()).isEqualTo("123456");
    assertThat(message.channelOrder()).containsExactly(OtpChannel.ZALO, OtpChannel.SMS);
    assertThat(message.expiryMinutes()).isEqualTo(5);

    assertThat(response.getChannel()).isEqualTo(OtpChannel.ZALO.getValue());
    assertThat(response.getTtlSeconds()).isEqualTo(300);
    assertThat(response.getRequestId()).isNotBlank();
  }

  @Test
  void sendOtp_doesNotEnqueue_whenStoreFails() {
    when(phoneNumberUtil.normalizeAndValidate(anyString())).thenReturn("+84900000000");
    when(phoneNumberUtil.maskPhoneNumber(anyString())).thenReturn("masked");
    when(otpUtil.generateOtpCode()).thenReturn("123456");
    when(otpUtil.hashOtpCode(anyString())).thenReturn("hashed");
    when(otpProperties.getTtlSeconds()).thenReturn(300);
    when(otpProperties.getMaxVerificationAttempts()).thenReturn(5);
    when(otpStore.store(any(OtpData.class), eq(300))).thenReturn(false);

    OtpSendRequest request = OtpSendRequest.builder().phone("0900000000").build();

    assertThatThrownBy(() -> newService().sendOtp(request, "1.2.3.4"))
        .isInstanceOf(OtpException.class);
    verify(notificationPublisher, never()).publishOtp(any());
  }
}
