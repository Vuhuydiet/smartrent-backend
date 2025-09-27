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
@Schema(description = "Response object containing created user information")
public class UserCreationResponse {

  @Schema(
      description = "Unique identifier for the created user",
      example = "user-123e4567-e89b-12d3-a456-426614174000"
  )
  String userId;

  @Schema(
      description = "International phone code (country code)",
      example = "+1"
  )
  String phoneCode;

  @Schema(
      description = "Phone number without country code",
      example = "1234567890"
  )
  String phoneNumber;

  @Schema(
      description = "User's email address",
      example = "john.doe@example.com"
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
}
