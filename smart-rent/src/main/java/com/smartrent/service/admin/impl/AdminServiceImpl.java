package com.smartrent.service.admin.impl;

import com.smartrent.config.Constants;
import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
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

}
