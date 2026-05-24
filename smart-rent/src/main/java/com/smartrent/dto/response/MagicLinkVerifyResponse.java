package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Guest access token issued after a magic link is verified")
public class MagicLinkVerifyResponse {

  @Schema(
      description = "Short-lived JWT to send on the Authorization header as `Bearer <token>`. No refresh token is issued for guest sessions — request a new link when this expires.",
      example = "eyJhbGciOiJIUzUxMiJ9..."
  )
  String accessToken;

  @Schema(
      description = "Number of seconds the access token remains valid.",
      example = "3600"
  )
  long expiresInSeconds;

  @Schema(
      description = "Email tied to this guest session (same as the one used to request the link).",
      example = "guest@example.com"
  )
  String email;

  @Schema(
      description = "Always `true` for tokens issued through the magic-link flow. Useful for the FE to render a \"guest mode\" badge.",
      example = "true"
  )
  Boolean guest;
}
