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
@Schema(description = "Acknowledgement that a magic-link email was dispatched")
public class MagicLinkResponse {

  @Schema(
      description = "Email the magic link was sent to. Returned regardless of whether the user already has an account, so callers cannot use this endpoint to enumerate accounts.",
      example = "guest@example.com"
  )
  String email;

  @Schema(
      description = "Number of seconds the magic link remains valid after dispatch.",
      example = "600"
  )
  long expiresInSeconds;
}
