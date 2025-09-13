package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
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
public class ChangePasswordRequest {
  String oldPassword;

  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  String newPassword;

  String verificationCode;

}

