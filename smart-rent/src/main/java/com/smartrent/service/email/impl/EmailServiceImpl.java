package com.smartrent.service.email.impl;

import com.smartrent.infra.connector.BrevoConnector;
import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.service.email.EmailService;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class EmailServiceImpl implements EmailService {

  @NonFinal
  @Value("${application.api-key.brevo}")
  String apiKey;

  BrevoConnector brevoConnector;

  @Override
  public EmailResponse sendEmail(EmailRequest emailRequest) {
    try {
      return brevoConnector.sendEmail(apiKey, emailRequest);
    } catch (FeignException e){
      throw new DomainException(DomainCode.CANNOT_SEND_EMAIL);
    }
  }
}
