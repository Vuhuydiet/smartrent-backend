package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Request object for resetting forgotten password")
public class ForgotPasswordRequest {

  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  @Schema(
      description = "New password (minimum 8 characters, must contain uppercase, lowercase, number, and special character)",
      example = "NewSecurePass123!",
      minLength = 8,
      required = true
  )
  String newPassword;

  @Schema(
      description = "6-digit verification code sent to user's email",
      example = "123456",
      pattern = "^[0-9]{6}$",
      required = true
  )
  String verificationCode;
}
