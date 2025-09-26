package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
  @Schema(
      description = "6-digit verification code sent to user's email",
      example = "123456",
      pattern = "^[0-9]{6}$",
      required = true
  )
  @NotBlank
  String verificationCode;
}
