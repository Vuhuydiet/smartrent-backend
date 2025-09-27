package com.smartrent.service.authentication;

import com.smartrent.dto.request.VerifyCodeRequest;
import com.smartrent.infra.repository.entity.User;

public interface VerificationService {

  void verifyCode(VerifyCodeRequest verifyCodeRequest);

  User verifyCode(String verifyCode);

  void sendVerifyCode(String email);

}
