package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request a guest magic-link login email")
public class MagicLinkRequest {

  @NotBlank(message = "INVALID_EMAIL")
  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  @Schema(
      description = "Email address that should receive the login link",
      example = "guest@example.com",
      format = "email",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  String email;
}
