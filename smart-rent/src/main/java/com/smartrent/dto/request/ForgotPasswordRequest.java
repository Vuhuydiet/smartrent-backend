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
@Schema(description = "Request object for resetting forgotten password")
public class ForgotPasswordRequest {

  @Schema(
      description = "Email address of the user requesting password reset",
      example = "john.doe@example.com",
      format = "email",
      required = true
  )
  @NotBlank(message = "INVALID_EMAIL")
  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  String email;

  @Schema(
      description = "6-digit verification code sent to user's email",
      example = "123456",
      pattern = "^[0-9]{6}$",
      required = true
  )
  @NotBlank(message = "EMPTY_INPUT")
  @Pattern(regexp = "^[0-9]{6}$", message = "INVALID_VERIFICATION_CODE")
  String verificationCode;
}
