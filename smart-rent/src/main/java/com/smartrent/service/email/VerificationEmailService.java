package com.smartrent.service.email;

import com.smartrent.infra.repository.entity.VerifyCode;

public interface VerificationEmailService {
  VerifyCode sendCode(String id);
}
