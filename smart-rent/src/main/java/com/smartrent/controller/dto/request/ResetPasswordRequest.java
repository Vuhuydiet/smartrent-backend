package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request object for resetting password with token")
public class ResetPasswordRequest {

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "Reset password token obtained from forgot-password endpoint",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      required = true
  )
  String resetPasswordToken;

  @NotBlank(message = "EMPTY_INPUT")
  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  @Schema(
      description = "New password (minimum 8 characters, must contain uppercase, lowercase, number, and special character)",
      example = "NewSecurePass123!",
      minLength = 8,
      required = true
  )
  String newPassword;

}
