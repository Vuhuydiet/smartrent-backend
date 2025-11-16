package com.smartrent.service.role;

import com.smartrent.dto.request.RoleCreationRequest;
import com.smartrent.dto.request.RoleUpdateRequest;
import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.dto.response.PageResponse;
import java.util.List;


public interface RoleService {
  List<GetRoleResponse> getAllRoles();

  PageResponse<GetRoleResponse> getAllRoles(int page, int size);

  GetRoleResponse getRoleById(String roleId);

  GetRoleResponse createRole(RoleCreationRequest request);

  GetRoleResponse updateRole(String roleId, RoleUpdateRequest request);

  void deleteRole(String roleId);
}
