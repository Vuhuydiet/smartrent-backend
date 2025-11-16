package com.smartrent.service.admin;

import com.smartrent.dto.request.AdminCreationRequest;
import com.smartrent.dto.request.AdminUpdateRequest;
import com.smartrent.dto.response.AdminCreationResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.PageResponse;

public interface AdminService {
  AdminCreationResponse createAdmin(AdminCreationRequest request);

  GetAdminResponse getAdminById(String id);

  PageResponse<GetAdminResponse> getAllAdmins(int page, int size);

  GetAdminResponse updateAdmin(String adminId, AdminUpdateRequest request);

  void deleteAdmin(String adminId);
}
