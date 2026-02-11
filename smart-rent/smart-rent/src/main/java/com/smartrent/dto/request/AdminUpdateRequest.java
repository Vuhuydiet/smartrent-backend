package com.smartrent.dto.request;

import com.smartrent.config.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request object for updating an administrator account")
public class AdminUpdateRequest {

  @Size(min = 1, max = 5)
  @Schema(
      description = "International phone code (country code)",
      example = "+1",
      minLength = 1,
      maxLength = 5
  )
  String phoneCode;

  @Size(min = 5, max = 20)
  @Schema(
      description = "Phone number without country code",
      example = "9876543210",
      minLength = 5,
      maxLength = 20
  )
  String phoneNumber;

  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  @Schema(
      description = "Administrator's email address",
      example = "admin@smartrent.com",
      format = "email"
  )
  String email;

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
      description = "List of roles to assign to the administrator",
      example = "[\"ADMIN\", \"SUPER_ADMIN\"]"
  )
  List<String> roles;
}

