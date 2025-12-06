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
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request object for updating user's own profile (used internally, avatar is handled as file upload)")
public class UserProfileUpdateRequest {

  @Schema(
      description = "User's first name",
      example = "John"
  )
  String firstName;

  @Schema(
      description = "User's last name",
      example = "Doe"
  )
  String lastName;

  @Schema(
      description = "User's identification document number",
      example = "ID123456789"
  )
  String idDocument;

  @Schema(
      description = "User's tax identification number",
      example = "TAX987654321"
  )
  String taxNumber;

  @Schema(
      description = "Vietnam contact phone number for Zalo or other messaging (format: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx)",
      example = "0912345678"
  )
  String contactPhoneNumber;
}

