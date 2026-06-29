package com.smartrent.service.notification;

import com.smartrent.enums.OtpChannel;
import java.util.List;

/**
 * Payload for an {@link NotificationType#OTP} job. Carries everything the
 * worker needs to run the channel-fallback send (Zalo -> SMS) off the request
 * thread. The OTP itself has already been hashed and stored before enqueueing.
 */
public record OtpMessage(
    String phone,
    String otpCode,
    List<OtpChannel> channelOrder,
    String requestId,
    int expiryMinutes) {
}
