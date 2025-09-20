package com.smartrent.service.email;

import com.smartrent.infra.connector.model.EmailRequest;
import com.smartrent.infra.connector.model.EmailResponse;

public interface EmailService {
  EmailResponse sendEmail(EmailRequest emailRequest);
}
