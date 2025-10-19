package com.smartrent.service.user.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.exception.DocumentExistingException;
import com.smartrent.infra.exception.EmailExistingException;
import com.smartrent.infra.exception.PhoneExistingException;
import com.smartrent.infra.exception.TaxNumberExisting;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.authentication.OtpCacheService;
import com.smartrent.service.authentication.domain.OtpData;
import com.smartrent.service.email.VerificationEmailService;
import com.smartrent.service.user.UserService;
import com.smartrent.utility.MaskingUtil;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;

  PasswordEncoder passwordEncoder;

  UserMapper userMapper;

  VerificationEmailService verificationEmailService;

  OtpCacheService otpCacheService;

  @Override
  @Transactional
  public UserCreationResponse createUser(UserCreationRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new EmailExistingException();
    }

    if (StringUtils.isNotBlank(request.getPhoneCode()) && StringUtils.isNotBlank(request.getPhoneNumber()) &&
        userRepository.existsByPhoneCodeAndPhoneNumber(request.getPhoneCode(),
        request.getPhoneNumber())) {
      throw new PhoneExistingException();
    }

    if (StringUtils.isNotBlank(request.getIdDocument()) &&
        userRepository.existsByIdDocument(request.getIdDocument())) {
      throw new DocumentExistingException();
    }

    if (StringUtils.isNotBlank(request.getTaxNumber()) &&
        userRepository.existsByTaxNumber(request.getTaxNumber())) {
      throw new TaxNumberExisting();
    }

    User user = userMapper.mapFromUserCreationRequestToUserEntity(request);

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setVerified(false);

    userRepository.saveAndFlush(user);

    // Generate and send OTP, then store in cache
    OtpData otpData = verificationEmailService.sendCode(user.getUserId());
    otpCacheService.storeOtp(otpData);

    return userMapper.mapFromUserEntityToUserCreationResponse(user);
  }

  @Override
  @Cacheable(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#id")
  public GetUserResponse getUserById(String id) {
    User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

    log.info(
        "getUserById: id={}, firstName={}, lastName={}, phoneNumber={}, documentId={}, email={}",
        MaskingUtil.maskFromIndex(user.getUserId(), Constants.USER_ID_MASKING_INDEX),
        user.getFirstName(),
        user.getLastName(), MaskingUtil.maskFromIndex(user.getPhoneCode() + user.getPhoneNumber(),
            Constants.PHONE_MASKING_INDEX),
        MaskingUtil.maskFromIndex(user.getIdDocument(), Constants.ID_DOCUMENT_MASKING_INDEX),
        MaskingUtil.maskEmail(user.getEmail()));

    return userMapper.mapFromUserEntityToGetUserResponse(user);
  }

  @Override
  public User internalCreateUser(InternalUserCreationRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new EmailExistingException();
    }

    User user = userMapper.mapFromInternalUserCreationRequestToUserEntity(request);

    user.setVerified(true);

    userRepository.saveAndFlush(user);

    return user;
  }
}
