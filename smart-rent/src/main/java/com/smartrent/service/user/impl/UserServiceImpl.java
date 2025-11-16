package com.smartrent.service.user.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UpdateContactPhoneRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.request.UserUpdateRequest;
import com.smartrent.dto.response.GetUserResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.exception.InvalidPageException;
import com.smartrent.exception.InvalidPageSizeException;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  @Cacheable(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#page + #size")
  public PageResponse<GetUserResponse> getUsers(int page, int size) {
    // Validate pagination parameters
    if (page < 1) {
      throw new InvalidPageException();
    }
    if (size <= 0) {
      throw new InvalidPageSizeException();
    }

    Pageable pageable = PageRequest.of(page-1, size);

    Page<User> users = userRepository.findAll(pageable);

    List<GetUserResponse> userResponses = users.getContent().stream()
        .map(userMapper::mapFromUserEntityToGetUserResponse)
        .toList();

    return PageResponse.<GetUserResponse>builder()
        .page(page)
        .size(size)
        .totalElements(users.getTotalElements())
        .totalPages(users.getTotalPages())
        .data(userResponses)
        .build();
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

  @Override
  @Transactional
  @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId")
  public GetUserResponse updateContactPhone(String userId, UpdateContactPhoneRequest request) {
    log.info("Updating contact phone for user {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    // Update contact phone number
    user.setContactPhoneNumber(request.getContactPhoneNumber());
    // Reset verification status when phone is updated
    user.setContactPhoneVerified(false);

    userRepository.save(user);

    log.info("Contact phone updated successfully for user {}", userId);

    return userMapper.mapFromUserEntityToGetUserResponse(user);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId")
  public GetUserResponse updateUser(String userId, UserUpdateRequest request) {
    log.info("Updating user with ID: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("User not found with ID: {}", userId);
          return new UserNotFoundException();
        });

    // Update email if provided and different
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new EmailExistingException();
      }
      user.setEmail(request.getEmail());
    }

    // Update first name if provided
    if (request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }

    // Update last name if provided
    if (request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }

    // Update ID document if provided and different
    if (request.getIdDocument() != null && !request.getIdDocument().equals(user.getIdDocument())) {
      if (userRepository.existsByIdDocument(request.getIdDocument())) {
        throw new DocumentExistingException();
      }
      user.setIdDocument(request.getIdDocument());
    }

    // Update tax number if provided and different
    if (request.getTaxNumber() != null && !request.getTaxNumber().equals(user.getTaxNumber())) {
      if (userRepository.existsByTaxNumber(request.getTaxNumber())) {
        throw new TaxNumberExisting();
      }
      user.setTaxNumber(request.getTaxNumber());
    }

    // Update contact phone number if provided
    if (request.getContactPhoneNumber() != null) {
      user.setContactPhoneNumber(request.getContactPhoneNumber());
      // Reset verification status when phone is updated
      user.setContactPhoneVerified(false);
    }

    // Update verification status if provided
    if (request.getIsVerified() != null) {
      user.setVerified(request.getIsVerified());
    }

    user = userRepository.saveAndFlush(user);
    log.info("Successfully updated user: {}", userId);

    return userMapper.mapFromUserEntityToGetUserResponse(user);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = Constants.CacheNames.USER_DETAILS, key = "#userId")
  public void deleteUser(String userId) {
    log.info("Deleting user with ID: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("User not found with ID: {}", userId);
          return new UserNotFoundException();
        });

    userRepository.delete(user);
    log.info("Successfully deleted user: {}", userId);
  }
}
