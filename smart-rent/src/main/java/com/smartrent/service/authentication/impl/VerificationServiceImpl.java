package com.smartrent.service.authentication.impl;

import com.smartrent.controller.dto.request.VerifyCodeRequest;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.exception.VerifyCodeExpiredException;
import com.smartrent.infra.exception.VerifyCodeNotFoundException;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.VerifyCodeRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.VerifyCode;
import com.smartrent.service.authentication.VerificationService;
import com.smartrent.service.email.VerificationEmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerificationServiceImpl implements VerificationService {

  VerifyCodeRepository verifyCodeRepository;

  UserRepository userRepository;

  VerificationEmailService verificationEmailService;

  @Override
  public void verifyCode(VerifyCodeRequest verifyCodeRequest) {
    VerifyCode verifyCode = checkExisted(verifyCodeRequest.getUserId(), verifyCodeRequest.getVerificationCode());

    if (checkExpiration(verifyCode)) {
      verifyCodeRepository.delete(verifyCode);
      throw new VerifyCodeExpiredException();
    }

    User user = verifyCode.getUser();
    user.setVerified(true);
    userRepository.saveAndFlush(user);

    verifyCodeRepository.delete(verifyCode);
  }

  @Override
  public User verifyCode(String verifyCode) {
    VerifyCode verifyCodeEntity = verifyCodeRepository.findById(verifyCode).orElseThrow(VerifyCodeNotFoundException::new);

    if (checkExpiration(verifyCodeEntity)) {
      verifyCodeRepository.delete(verifyCodeEntity);
      throw new VerifyCodeExpiredException();
    }

    User user = verifyCodeEntity.getUser();
    user.setVerified(true);
    userRepository.saveAndFlush(user);
    verifyCodeRepository.delete(verifyCodeEntity);

    return user;
  }

  @Override
  public void sendVerifyCode(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(UserNotFoundException::new);
    VerifyCode verifyCode = verificationEmailService.sendCode(user.getUserId());

    verifyCodeRepository.deleteByUserId(user.getUserId());
    verifyCodeRepository.saveAndFlush(verifyCode);
  }

  private VerifyCode checkExisted(String userId, String verificationCode) {
    return verifyCodeRepository.findByVerifyCodeAndUserId(verificationCode, userId)
        .orElseThrow(VerifyCodeNotFoundException::new);
  }

  private boolean checkExpiration(VerifyCode verifyCode) {
    return verifyCode.getExpirationTime().isBefore(java.time.LocalDateTime.now());
  }
}
