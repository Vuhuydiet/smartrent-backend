package com.smartrent.service.email.impl;

import com.smartrent.infra.connector.BrevoConnector;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;
import com.smartrent.service.email.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class BrevoEmailServiceImpl implements EmailService {

  @NonFinal
  @Value("${application.api-key.brevo}")
  String apiKey;

  BrevoConnector brevoConnector;

  @Override
  public EmailResponse sendEmail(EmailRequest emailRequest) {
      return brevoConnector.sendEmail(apiKey, emailRequest);
  }
}
