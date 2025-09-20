package com.smartrent.service.role;

import com.smartrent.controller.dto.response.GetRoleResponse;
import java.util.List;


public interface RoleService {
  List<GetRoleResponse> getAllRoles();
}
