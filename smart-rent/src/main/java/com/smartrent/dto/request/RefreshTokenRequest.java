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
@Schema(description = "Request object for refreshing access tokens")
public class RefreshTokenRequest {

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "JWT refresh token used to generate new access token",
      example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      required = true
  )
  String refreshToken;
}
