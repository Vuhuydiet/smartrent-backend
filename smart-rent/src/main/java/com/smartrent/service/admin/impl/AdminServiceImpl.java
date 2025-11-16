package com.smartrent.service.admin.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.request.AdminUpdateRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.infra.exception.AdminNotFoundException;
import com.smartrent.infra.exception.EmailExistingException;
import com.smartrent.infra.exception.InvalidRoleException;
import com.smartrent.infra.exception.PhoneExistingException;
import com.smartrent.infra.exception.UserNotFoundException;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.RoleRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.AdminMapper;
import com.smartrent.service.admin.AdminService;
import com.smartrent.utility.MaskingUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminServiceImpl implements AdminService {

  AdminRepository adminRepository;

  RoleRepository roleRepository;

  PasswordEncoder passwordEncoder;

  AdminMapper adminMapper;

  @Override
  public AdminCreationResponse createAdmin(AdminCreationRequest request) {

    if (adminRepository.existsByEmail(request.getEmail())) {
      throw new EmailExistingException();
    }

    if (adminRepository.existsByPhoneCodeAndPhoneNumber(request.getPhoneCode(),
        request.getPhoneNumber())) {
      throw new PhoneExistingException();
    }

    Admin admin = adminMapper.mapFromAdminCreationRequestToAminEntity(request);

    admin.setPassword(passwordEncoder.encode(request.getPassword()));

    List<Role> roles = roleRepository.getRolesByRoleIdIn(request.getRoles());

    if (roles.size() < request.getRoles().size()) {
      throw new InvalidRoleException();
    }

    admin.setRoles(roles);

    adminRepository.saveAndFlush(admin);

    return adminMapper.mapFromAdminEntityToAdminCreationResponse(admin);
  }

  @Override
  public GetAdminResponse getAdminById(String id) {
    Admin admin = adminRepository.findById(id).orElseThrow(UserNotFoundException::new);

    log.info(
        "getUserById: id={}, firstName={}, lastName={}, phoneNumber={}, email={}",
        MaskingUtil.maskFromIndex(admin.getAdminId(), Constants.USER_ID_MASKING_INDEX),
        admin.getFirstName(),
        admin.getLastName(), MaskingUtil.maskFromIndex(admin.getPhoneCode() + admin.getPhoneNumber(),
            Constants.PHONE_MASKING_INDEX),
        MaskingUtil.maskEmail(admin.getEmail()));

    return adminMapper.mapFromAdminEntityToGetAdminResponse(admin);
  }

  @Override
  public PageResponse<GetAdminResponse> getAllAdmins(int page, int size) {
    log.info("Fetching all admins - page: {}, size: {}", page, size);

    Pageable pageable = PageRequest.of(page - 1, size);
    Page<Admin> adminPage = adminRepository.findAll(pageable);

    List<GetAdminResponse> adminResponses = adminPage.getContent().stream()
        .map(adminMapper::mapFromAdminEntityToGetAdminResponse)
        .collect(Collectors.toList());

    log.info("Successfully retrieved {} admins", adminResponses.size());

    return PageResponse.<GetAdminResponse>builder()
        .page(page)
        .size(adminPage.getSize())
        .totalPages(adminPage.getTotalPages())
        .totalElements(adminPage.getTotalElements())
        .data(adminResponses)
        .build();
  }

  @Override
  @Transactional
  public GetAdminResponse updateAdmin(String adminId, AdminUpdateRequest request) {
    log.info("Updating admin with ID: {}", adminId);

    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> {
          log.error("Admin not found with ID: {}", adminId);
          return new AdminNotFoundException();
        });

    // Update email if provided and different
    if (request.getEmail() != null && !request.getEmail().equals(admin.getEmail())) {
      if (adminRepository.existsByEmail(request.getEmail())) {
        throw new EmailExistingException();
      }
      admin.setEmail(request.getEmail());
    }

    // Update phone if provided and different
    if (request.getPhoneCode() != null && request.getPhoneNumber() != null) {
      if (!request.getPhoneCode().equals(admin.getPhoneCode())
          || !request.getPhoneNumber().equals(admin.getPhoneNumber())) {
        if (adminRepository.existsByPhoneCodeAndPhoneNumber(
            request.getPhoneCode(), request.getPhoneNumber())) {
          throw new PhoneExistingException();
        }
        admin.setPhoneCode(request.getPhoneCode());
        admin.setPhoneNumber(request.getPhoneNumber());
      }
    }

    // Update first name if provided
    if (request.getFirstName() != null) {
      admin.setFirstName(request.getFirstName());
    }

    // Update last name if provided
    if (request.getLastName() != null) {
      admin.setLastName(request.getLastName());
    }

    // Update roles if provided
    if (request.getRoles() != null && !request.getRoles().isEmpty()) {
      List<Role> roles = roleRepository.getRolesByRoleIdIn(request.getRoles());
      if (roles.size() < request.getRoles().size()) {
        throw new InvalidRoleException();
      }
      admin.setRoles(roles);
    }

    admin = adminRepository.saveAndFlush(admin);
    log.info("Successfully updated admin: {}", adminId);

    return adminMapper.mapFromAdminEntityToGetAdminResponse(admin);
  }

  @Override
  @Transactional
  public void deleteAdmin(String adminId) {
    log.info("Deleting admin with ID: {}", adminId);

    Admin admin = adminRepository.findById(adminId)
        .orElseThrow(() -> {
          log.error("Admin not found with ID: {}", adminId);
          return new AdminNotFoundException();
        });

    adminRepository.delete(admin);
    log.info("Successfully deleted admin: {}", adminId);
  }

}
