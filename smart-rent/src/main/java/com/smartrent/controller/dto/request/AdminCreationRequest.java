package com.smartrent.controller.dto.request;

import com.smartrent.config.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AdminCreationRequest {

  @Size(min = 1, max = 5)
  String phoneCode;

  @Size(min = 5, max = 20)
  String phoneNumber;

  @NotBlank
  @Pattern(regexp = Constants.EMAIL_PATTERN, message = "INVALID_EMAIL")
  String email;

  @Size(min = 8, message = "INVALID_PASSWORD")
  @Pattern(regexp = Constants.PASSWORD_PATTERN, message = "INVALID_PASSWORD")
  String password;

  @NotBlank
  String firstName;

  @NotBlank
  String lastName;

  @NotNull
  List<String> roles;
}
