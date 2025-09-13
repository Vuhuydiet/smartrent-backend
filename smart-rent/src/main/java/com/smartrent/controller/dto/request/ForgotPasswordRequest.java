package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ForgotPasswordRequest {
  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  String newPassword;

  String verificationCode;
}
