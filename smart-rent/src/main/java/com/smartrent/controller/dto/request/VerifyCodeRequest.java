package com.smartrent.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request object for email verification")
public class VerifyCodeRequest {

  @Schema(
      description = "Email address to verify",
      example = "john.doe@example.com",
      format = "email",
      required = true
  )
  String email;

  @Schema(
      description = "6-digit verification code sent to the email",
      example = "123456",
      pattern = "^[0-9]{6}$",
      required = true
  )
  String verificationCode;
}
