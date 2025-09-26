package com.smartrent.dto.request;

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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request object for user logout")
public class LogoutRequest {

  @Schema(
      description = "JWT access token to invalidate",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      required = true
  )
  String token;
}
