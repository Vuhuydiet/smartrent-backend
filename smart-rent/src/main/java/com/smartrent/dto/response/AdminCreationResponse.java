package com.smartrent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@Schema(description = "Response object containing created administrator information")
public class AdminCreationResponse {

  @Schema(
      description = "Unique identifier for the created administrator",
      example = "admin-123e4567-e89b-12d3-a456-426614174000"
  )
  String adminId;

  @Schema(
      description = "International phone code (country code)",
      example = "+1"
  )
  String phoneCode;

  @Schema(
      description = "Phone number without country code",
      example = "9876543210"
  )
  String phoneNumber;

  @Schema(
      description = "Administrator's email address",
      example = "admin@smartrent.com"
  )
  String email;

  @Schema(
      description = "Administrator's password (returned for initial setup)",
      example = "AdminPass123!"
  )
  String password;

  @Schema(
      description = "Administrator's first name",
      example = "Jane"
  )
  String firstName;

  @Schema(
      description = "Administrator's last name",
      example = "Smith"
  )
  String lastName;

  @Schema(
      description = "List of roles assigned to the administrator",
      example = "[\"ADMIN\", \"SUPER_ADMIN\"]"
  )
  List<String> roles;

}
