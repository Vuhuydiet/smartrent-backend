package com.smartrent.service.email;

import com.smartrent.service.authentication.domain.OtpData;

public interface VerificationEmailService {
  OtpData sendCode(String id);
}
