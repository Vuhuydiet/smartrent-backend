package com.smartrent.service.authentication;

import com.smartrent.controller.dto.request.VerifyCodeRequest;
import com.smartrent.infra.repository.entity.User;

public interface VerificationService {

  void verifyCode(VerifyCodeRequest verifyCodeRequest);

  User verifyCode(String verifyCode, String email);

  void sendVerifyCode(String email);

}
