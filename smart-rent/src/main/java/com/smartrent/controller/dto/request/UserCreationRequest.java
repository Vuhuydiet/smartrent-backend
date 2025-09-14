package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Request object for creating a new user account")
public class UserCreationRequest {

  @NotBlank(message = "INVALID_PHONE")
  @Size(min = 1, max = 5)
  @Schema(
      description = "International phone code (country code)",
      example = "+1",
      minLength = 1,
      maxLength = 5,
      required = true
  )
  String phoneCode;

  @NotBlank(message = "INVALID_PHONE")
  @Size(min = 5, max = 20)
  @Schema(
      description = "Phone number without country code",
      example = "1234567890",
      minLength = 5,
      maxLength = 20,
      required = true
  )
  String phoneNumber;

  @NotBlank
  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  @Schema(
      description = "User's email address",
      example = "john.doe@example.com",
      format = "email",
      required = true
  )
  String email;

  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  @Schema(
      description = "User's password (minimum 8 characters, must contain uppercase, lowercase, number, and special character)",
      example = "SecurePass123!",
      minLength = 8,
      required = true
  )
  String password;

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "User's first name",
      example = "John",
      required = true
  )
  String firstName;

  @NotBlank(message = "EMPTY_INPUT")
  @Schema(
      description = "User's last name",
      example = "Doe",
      required = true
  )
  String lastName;

  @Schema(
      description = "User's identification document number (optional)",
      example = "ID123456789"
  )
  String idDocument;

  @Schema(
      description = "User's tax identification number (optional)",
      example = "TAX987654321"
  )
  String taxNumber;
}
