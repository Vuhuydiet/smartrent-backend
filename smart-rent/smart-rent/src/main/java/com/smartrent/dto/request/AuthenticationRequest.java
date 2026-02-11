package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request object for user/admin authentication")
public class AuthenticationRequest {

  @NotBlank(message = "INVALID_EMAIL")
  @Schema(
      description = "User's email address",
      example = "john.doe@example.com",
      format = "email",
      required = true
  )
  String email;

  @NotBlank(message = "INVALID_PASSWORD")
  @Schema(
      description = "User's password",
      example = "SecurePass123!",
      required = true
  )
  String password;
}
