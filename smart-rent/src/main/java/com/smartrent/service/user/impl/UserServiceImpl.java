package com.smartrent.service.user.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.InternalUserCreationRequest;
import com.smartrent.dto.request.UpdateContactPhoneRequest;
import com.smartrent.dto.request.UserCreationRequest;
import com.smartrent.dto.request.UserProfileUpdateRequest;
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
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.storage.R2StorageService;
import com.smartrent.service.email.VerificationEmailService;
import com.smartrent.service.user.UserService;
import com.smartrent.utility.MaskingUtil;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.multipart.MultipartFile;
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

  R2StorageService storageService;

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
  public GetUserResponse updateUserProfile(String userId, UserProfileUpdateRequest request, MultipartFile avatarFile) {
    log.info("Updating profile for user {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    // Update first name if provided
    if (request != null && request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }

    // Update last name if provided
    if (request != null && request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }

    // Update ID document if provided and different
    if (request != null && request.getIdDocument() != null && !request.getIdDocument().equals(user.getIdDocument())) {
      if (userRepository.existsByIdDocument(request.getIdDocument())) {
        throw new DocumentExistingException();
      }
      user.setIdDocument(request.getIdDocument());
    }

    // Update tax number if provided and different
    if (request != null && request.getTaxNumber() != null && !request.getTaxNumber().equals(user.getTaxNumber())) {
      if (userRepository.existsByTaxNumber(request.getTaxNumber())) {
        throw new TaxNumberExisting();
      }
      user.setTaxNumber(request.getTaxNumber());
    }

    // Update contact phone number if provided
    if (request != null && request.getContactPhoneNumber() != null) {
      user.setContactPhoneNumber(request.getContactPhoneNumber());
      // Reset verification status when phone is updated
      user.setContactPhoneVerified(false);
    }

    // Upload avatar file to S3 if provided
    if (avatarFile != null && !avatarFile.isEmpty()) {
      String avatarUrl = uploadAvatarToStorage(userId, avatarFile);
      user.setAvatarUrl(avatarUrl);
    }

    user = userRepository.saveAndFlush(user);
    log.info("Successfully updated profile for user {}", userId);

    return userMapper.mapFromUserEntityToGetUserResponse(user);
  }

  /**
   * Upload avatar file to S3/R2 storage
   * @param userId User ID
   * @param avatarFile Avatar file to upload
   * @return Public URL of the uploaded avatar
   */
  private String uploadAvatarToStorage(String userId, MultipartFile avatarFile) {
    // Validate file type
    String contentType = avatarFile.getContentType();
    if (!storageService.isValidContentType(contentType, true)) {
      throw new AppException(DomainCode.INVALID_FILE_TYPE,
          "Invalid avatar file type. Allowed: image/jpeg, image/png, image/webp");
    }

    // Validate file size (max 5MB for avatar)
    long maxAvatarSize = 5 * 1024 * 1024; // 5MB
    if (avatarFile.getSize() > maxAvatarSize) {
      throw new AppException(DomainCode.FILE_TOO_LARGE,
          "Avatar file size must not exceed 5MB");
    }

    // Generate storage key for avatar
    String originalFilename = avatarFile.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String storageKey = String.format("avatars/%s/%s%s", userId, UUID.randomUUID(), extension);

    try {
      // Upload to S3/R2
      storageService.uploadFile(
          storageKey,
          avatarFile.getInputStream(),
          contentType,
          avatarFile.getSize()
      );

      // Return public URL
      String publicUrl = storageService.getPublicUrl(storageKey);
      log.info("Avatar uploaded successfully for user {}: {}", userId, publicUrl);
      return publicUrl;
    } catch (IOException e) {
      log.error("Failed to upload avatar for user {}", userId, e);
      throw new AppException(DomainCode.FILE_UPLOAD_ERROR, "Failed to upload avatar file");
    }
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

    // Update avatar URL if provided
    if (request.getAvatarUrl() != null) {
      user.setAvatarUrl(request.getAvatarUrl());
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
