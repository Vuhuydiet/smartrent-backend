package com.smartrent.mapper.impl;

import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.AdminMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminMapperImpl implements AdminMapper {

  @Override
  public Admin mapFromAdminCreationRequestToAminEntity(AdminCreationRequest adminCreationRequest) {
    return Admin.builder()
        .email(adminCreationRequest.getEmail())
        .password(adminCreationRequest.getPassword())
        .firstName(adminCreationRequest.getFirstName())
        .lastName(adminCreationRequest.getLastName())
        .phoneCode(adminCreationRequest.getPhoneCode())
        .phoneNumber(adminCreationRequest.getPhoneNumber())
        .build();
  }

  @Override
  public AdminCreationResponse mapFromAdminEntityToAdminCreationResponse(Admin admin) {
    return AdminCreationResponse.builder()
        .adminId(admin.getAdminId())
        .email(admin.getEmail())
        .firstName(admin.getFirstName())
        .lastName(admin.getLastName())
        .phoneCode(admin.getPhoneCode())
        .phoneNumber(admin.getPhoneNumber())
        .roles(admin.getRoles().stream().map(Role::getRoleName).toList())
        .build();
  }

  @Override
  public GetAdminResponse mapFromAdminEntityToGetAdminResponse(Admin admin) {
    return GetAdminResponse.builder()
        .adminId(admin.getAdminId())
        .email(admin.getEmail())
        .firstName(admin.getFirstName())
        .lastName(admin.getLastName())
        .phoneCode(admin.getPhoneCode())
        .phoneNumber(admin.getPhoneNumber())
        .roles(admin.getRoles().stream().map(Role::getRoleName).toList())
        .build();
  }
}
