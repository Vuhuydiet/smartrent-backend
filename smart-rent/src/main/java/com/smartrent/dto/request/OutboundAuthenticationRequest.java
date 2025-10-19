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
@Schema(description = "Request object for outbound authentication (OAuth login)")
public class OutboundAuthenticationRequest {

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "Authorization code received from OAuth provider",
      example = "4/0AY0e-g7xxxxxxxxxxxxxxxxxxxxxxxxxxx",
      required = true
  )
  String code;
}

