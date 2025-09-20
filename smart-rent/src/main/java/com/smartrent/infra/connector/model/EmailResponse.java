package com.smartrent.infra.connector.model;

import lombok.Builder;

@Builder
public class EmailResponse {
  String code;
  String message;
}
