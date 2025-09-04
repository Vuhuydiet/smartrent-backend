package com.smartrent.service.admin;

import com.smartrent.controller.dto.request.AdminCreationRequest;
import com.smartrent.controller.dto.response.AdminCreationResponse;
import com.smartrent.controller.dto.response.GetAdminResponse;

public interface AdminService {
  AdminCreationResponse createAdmin(AdminCreationRequest request);

  GetAdminResponse getAdminById(String id);
}
