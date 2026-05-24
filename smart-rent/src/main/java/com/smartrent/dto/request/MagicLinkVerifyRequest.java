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
@Schema(description = "Exchange a magic-link token for a guest access token")
public class MagicLinkVerifyRequest {

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "The single-use magic-link token extracted from the email URL's query string.",
      example = "eyJhbGciOiJIUzUxMiJ9...",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  String token;
}
