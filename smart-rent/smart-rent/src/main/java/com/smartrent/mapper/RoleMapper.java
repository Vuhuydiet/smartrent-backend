package com.smartrent.mapper;

import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.infra.repository.entity.Role;

public interface RoleMapper {
  GetRoleResponse mapFromRoleEntityToGetRoleResponse(Role role);
}
