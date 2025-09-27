package com.smartrent.service.role.impl;

import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.infra.repository.RoleRepository;
import com.smartrent.infra.repository.entity.Role;
import com.smartrent.mapper.RoleMapper;
import com.smartrent.service.role.RoleService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

  RoleRepository roleRepository;

  RoleMapper roleMapper;

  @Override
  public List<GetRoleResponse> getAllRoles() {
    log.info("Fetching all roles from database");

    try {
      List<Role> roles = roleRepository.findAll();
      log.info("Successfully retrieved {} roles", roles.size());

      List<GetRoleResponse> response = roles.stream()
          .map(roleMapper::mapFromRoleEntityToGetRoleResponse)
          .collect(Collectors.toList());

      log.debug("Mapped {} role entities to response DTOs", response.size());
      return response;

    } catch (Exception e) {
      log.error("Failed to retrieve roles from database", e);
      throw e; // Re-throw to let the controller handle it
    }
  }
}
