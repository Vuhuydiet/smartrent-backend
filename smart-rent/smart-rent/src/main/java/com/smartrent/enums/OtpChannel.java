package com.smartrent.enums;

import lombok.Getter;

/**
 * Enum representing OTP delivery channels
 */
@Getter
public enum OtpChannel {
    ZALO("zalo", "Zalo ZNS"),
    SMS("sms", "SMS");

    private final String value;
    private final String displayName;

    OtpChannel(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static OtpChannel fromValue(String value) {
        for (OtpChannel channel : values()) {
            if (channel.value.equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown OTP channel: " + value);
    }
}

