package com.smartrent.controller.dto.response;

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
public class AdminCreationResponse {

  String adminId;

  String phoneCode;

  String phoneNumber;

  String email;

  String password;

  String firstName;

  String lastName;

  String idDocument;

  String taxNumber;

  List<String> roles;

}
