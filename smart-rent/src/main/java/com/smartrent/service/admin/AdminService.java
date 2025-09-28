package com.smartrent.service.admin;

import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;

public interface AdminService {
  AdminCreationResponse createAdmin(AdminCreationRequest request);

  GetAdminResponse getAdminById(String id);
}
