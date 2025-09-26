package com.smartrent.mapper.impl;

import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.RoleMapper;
import org.springframework.stereotype.Component;

@Component
public class RoleMapperImpl implements RoleMapper {

  @Override
  public GetRoleResponse mapFromRoleEntityToGetRoleResponse(Role role) {
    return GetRoleResponse.builder()
        .roleId(role.getRoleId())
        .roleName(role.getRoleName())
        .build();
  }
}
