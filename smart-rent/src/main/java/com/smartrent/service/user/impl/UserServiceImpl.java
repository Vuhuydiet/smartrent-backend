package com.smartrent.service.user.impl;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.UserCreationRequest;
import com.smartrent.controller.dto.response.GetUserResponse;
import com.smartrent.controller.dto.response.UserCreationResponse;
import com.smartrent.infra.exception.DocumentExistingException;
import com.smartrent.infra.exception.EmailExistingException;
import com.smartrent.infra.exception.PhoneExistingException;
import com.smartrent.infra.exception.TaxNumberExisting;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.VerifyCodeRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.infra.repository.entity.VerifyCode;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.email.VerificationEmailService;
import com.smartrent.service.user.UserService;
import com.smartrent.utility.MaskingUtil;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;

  PasswordEncoder passwordEncoder;

  UserMapper userMapper;

  VerificationEmailService verificationEmailService;

  VerifyCodeRepository verifyCodeRepository;

  @Override
  @Transactional
  public UserCreationResponse createUser(UserCreationRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new EmailExistingException();
    }

    if (userRepository.existsByPhoneCodeAndPhoneNumber(request.getPhoneCode(),
        request.getPhoneNumber())) {
      throw new PhoneExistingException();
    }

    if (userRepository.existsByIdDocument(request.getIdDocument())) {
      throw new DocumentExistingException();
    }

    if (userRepository.existsByTaxNumber(request.getTaxNumber())) {
      throw new TaxNumberExisting();
    }

    User user = userMapper.mapFromUserCreationRequestToUserEntity(request);

    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setVerified(false);

    userRepository.saveAndFlush(user);

    VerifyCode verifyCode = verificationEmailService.sendCode(user.getUserId());

    verifyCodeRepository.saveAndFlush(verifyCode);

    return userMapper.mapFromUserEntityToUserCreationResponse(user);
  }

  @Override
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
}
