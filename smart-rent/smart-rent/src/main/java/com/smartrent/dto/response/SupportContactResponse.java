package com.smartrent.dto.response;

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
@Schema(description = "Response object containing admin support contact information")
public class SupportContactResponse {

  @Schema(
      description = "Admin's Zalo phone number for support contact",
      example = "0123456789"
  )
  String adminZaloPhoneNumber;

  @Schema(
      description = "Direct Zalo link to contact admin",
      example = "https://zalo.me/0123456789"
  )
  String zaloLink;
}

