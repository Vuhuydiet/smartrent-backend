package com.smartrent.mapper;

import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.JwtAdminClaimsDto;
import com.smartrent.infra.repository.entity.Admin;

public interface AdminMapper {
  Admin mapFromAdminCreationRequestToAminEntity(AdminCreationRequest adminCreationRequest);

  AdminCreationResponse mapFromAdminEntityToAdminCreationResponse(Admin admin);

  GetAdminResponse mapFromAdminEntityToGetAdminResponse(Admin admin);

  JwtAdminClaimsDto mapFromAdminEntityToJwtAdminClaimsDto(Admin admin);
}
