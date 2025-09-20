package com.smartrent.service.role.impl;

import com.smartrent.controller.dto.response.GetRoleResponse;
import com.smartrent.infra.repository.RoleRepository;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.RoleMapper;
import com.smartrent.service.role.RoleService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

  RoleRepository roleRepository;

  RoleMapper roleMapper;

  @Override
  public List<GetRoleResponse> getAllRoles() {
    List<Role> roles = roleRepository.findAll();

    return roles.stream().map(roleMapper::mapFromRoleEntityToGetRoleResponse).collect(Collectors.toList());
  }
}
