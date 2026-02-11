package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request object for updating a user account")
public class UserUpdateRequest {

  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  @Schema(
      description = "User's email address",
      example = "john.doe@example.com",
      format = "email"
  )
  String email;

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

  @Pattern(regexp = Constants.VIETNAM_PHONE_PATTERN, message = "INVALID_CONTACT_PHONE")
  @Schema(
      description = "Vietnam contact phone number for Zalo or other messaging (format: 09xxxxxxxx, 03xxxxxxxx, 07xxxxxxxx, 08xxxxxxxx, 05xxxxxxxx)",
      example = "0912345678"
  )
  String contactPhoneNumber;

  @Schema(
      description = "Whether the user is verified",
      example = "true"
  )
  Boolean isVerified;

  @Schema(
      description = "URL of the user's profile picture",
      example = "https://example.com/avatar.jpg"
  )
  String avatarUrl;
}

