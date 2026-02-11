package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Schema(description = "Request object for changing user password")
public class ChangePasswordRequest {

  @Schema(
      description = "Current password of the user",
      example = "OldPass123!",
      required = true
  )
  String oldPassword;

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

