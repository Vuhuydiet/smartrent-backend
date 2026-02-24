package com.smartrent.infra.connector.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Getter
@ToString
public class EmailInfo {
  String name;
  String email;
}

