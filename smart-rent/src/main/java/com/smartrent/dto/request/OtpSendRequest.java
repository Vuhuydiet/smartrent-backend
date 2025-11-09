package com.smartrent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for sending OTP
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    /**
     * Preferred channels in order of preference (optional)
     * Default: ["zalo", "sms"]
     */
    private List<String> preferredChannels;
}

