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
@Schema(description = "Tokens issued after a magic link is verified. Shape depends on whether the email maps to an existing user: registered users receive a full access+refresh pair (`guest=false`); unknown emails receive a short-lived guest access token only (`guest=true`).")
public class MagicLinkVerifyResponse {

  @Schema(
      description = "JWT to send on the Authorization header as `Bearer <token>`.",
      example = "eyJhbGciOiJIUzUxMiJ9..."
  )
  String accessToken;

  @Schema(
      description = "Refresh token. Returned only when the email matches a registered user (`guest=false`). Null/omitted for guest sessions — guests must request a new magic link when their access token expires.",
      example = "eyJhbGciOiJIUzUxMiJ9..."
  )
  String refreshToken;

  @Schema(
      description = "Number of seconds the access token remains valid.",
      example = "3600"
  )
  long expiresInSeconds;

  @Schema(
      description = "Email tied to this session (same as the one used to request the link).",
      example = "user@example.com"
  )
  String email;

  @Schema(
      description = "`true` when no matching user was found and a guest session was issued; `false` when the email matches a registered user and a full session was issued.",
      example = "false"
  )
  Boolean guest;

  @Schema(
      description = "Registered user's ID. Present only when `guest=false`. Useful for FE to fetch profile after login.",
      example = "550e8400-e29b-41d4-a716-446655440000"
  )
  String userId;
}
