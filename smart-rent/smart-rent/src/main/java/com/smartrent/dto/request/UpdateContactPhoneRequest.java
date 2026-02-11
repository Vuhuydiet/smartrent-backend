package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to update user's contact phone number")
public class UpdateContactPhoneRequest {

    @NotBlank(message = "Contact phone number is required")
    @Pattern(regexp = Constants.VIETNAM_PHONE_PATTERN, message = "INVALID_CONTACT_PHONE")
    @Schema(
            description = "Vietnam contact phone number for Zalo or other messaging (format: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx)",
            example = "0912345678",
            required = true
    )
    String contactPhoneNumber;
}

