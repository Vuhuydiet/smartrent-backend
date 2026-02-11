package com.smartrent.service.authentication.impl;

import com.smartrent.dto.request.VerifyCodeRequest;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.exception.VerifyCodeExpiredException;
import com.smartrent.infra.exception.VerifyCodeNotFoundException;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.authentication.OtpCacheService;
import com.smartrent.service.authentication.VerificationService;
import com.smartrent.service.authentication.domain.OtpData;
import com.smartrent.service.email.VerificationEmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerificationServiceImpl implements VerificationService {

  OtpCacheService otpCacheService;

  UserRepository userRepository;

  VerificationEmailService verificationEmailService;

  @Override
  public void verifyCode(VerifyCodeRequest verifyCodeRequest) {
    OtpData otpData = otpCacheService.getOtp(verifyCodeRequest.getVerificationCode(), verifyCodeRequest.getEmail())
        .orElseThrow(VerifyCodeNotFoundException::new);

    if (otpData.isExpired()) {
      otpCacheService.removeOtp(otpData.getUserId(), otpData.getOtpCode());
      throw new VerifyCodeExpiredException();
    }

    User user = userRepository.findById(otpData.getUserId())
        .orElseThrow(UserNotFoundException::new);
    user.setVerified(true);
    userRepository.saveAndFlush(user);

    otpCacheService.removeOtp(otpData.getUserId(), otpData.getOtpCode());
  }

  @Override
  public User verifyCode(String verifyCode, String email) {
    OtpData otpData = otpCacheService.getOtp(verifyCode, email)
        .orElseThrow(VerifyCodeNotFoundException::new);

    if (otpData.isExpired()) {
      otpCacheService.removeOtp(otpData.getUserId(), otpData.getOtpCode());
      throw new VerifyCodeExpiredException();
    }

    User user = userRepository.findById(otpData.getUserId())
        .orElseThrow(UserNotFoundException::new);

    otpCacheService.removeOtp(otpData.getUserId(), otpData.getOtpCode());

    return user;
  }

  @Override
  public void sendVerifyCode(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(UserNotFoundException::new);

    otpCacheService.removeUserOtps(user.getUserId());

    OtpData otpData = verificationEmailService.sendCode(user.getUserId());

    otpCacheService.storeOtp(otpData);
  }


}
