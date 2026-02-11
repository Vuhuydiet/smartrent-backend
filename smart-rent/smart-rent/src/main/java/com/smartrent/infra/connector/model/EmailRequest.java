package com.smartrent.infra.connector.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Builder
@Getter
public class EmailRequest {
  EmailInfo sender;
  List<EmailInfo> to;
  String subject;
  String htmlContent;
}
