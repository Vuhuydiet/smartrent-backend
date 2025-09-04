package com.smartrent.mapper;

import com.smartrent.controller.dto.request.AdminCreationRequest;
import com.smartrent.controller.dto.response.AdminCreationResponse;
import com.smartrent.controller.dto.response.GetAdminResponse;
import com.smartrent.infra.repository.entity.Admin;

public interface AdminMapper {
  Admin mapFromAdminCreationRequestToAminEntity(AdminCreationRequest adminCreationRequest);

  AdminCreationResponse mapFromAdminEntityToAdminCreationResponse(Admin admin);

  GetAdminResponse mapFromAdminEntityToGetAdminResponse(Admin admin);
}
