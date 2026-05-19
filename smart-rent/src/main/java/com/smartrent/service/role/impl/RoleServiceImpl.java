package com.smartrent.service.role.impl;

import com.smartrent.dto.request.AdminFilterRequest;
import com.smartrent.dto.request.RoleCreationRequest;
import com.smartrent.dto.request.RoleUpdateRequest;
import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.infra.exception.RoleExistingException;
import com.smartrent.infra.exception.RoleNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Override
  public PageResponse<GetRoleResponse> getAllRoles(int page, int size) {
    log.info("Fetching roles with pagination - page: {}, size: {}", page, size);

    try {
      Pageable pageable = PageRequest.of(page - 1, size);
      Page<Role> rolePage = roleRepository.findAll(pageable);

      List<GetRoleResponse> roleResponses = rolePage.getContent().stream()
          .map(roleMapper::mapFromRoleEntityToGetRoleResponse)
          .collect(Collectors.toList());

      log.info("Successfully retrieved {} roles", roleResponses.size());

      return PageResponse.<GetRoleResponse>builder()
          .page(page)
          .size(rolePage.getSize())
          .totalPages(rolePage.getTotalPages())
          .totalElements(rolePage.getTotalElements())
          .data(roleResponses)
          .build();

    } catch (Exception e) {
      log.error("Failed to retrieve roles from database", e);
      throw e;
    }
  }

  @Override
  public PageResponse<GetRoleResponse> getAllRoles(AdminFilterRequest filter) {
    int page = Math.max(filter.getPage() != null ? filter.getPage() : 1, 1);
    int size = Math.max(filter.getSize() != null ? filter.getSize() : 20, 1);

    log.info("Fetching roles with filters - page: {}, size: {}, filters: {}", page, size, filter.getFilters());

    try {
      List<Role> roles = roleRepository.findAll();

      // Filter by roleId if provided
      if (filter.hasFilter("roleId")) {
        String roleIdFilter = filter.getStringFilter("roleId").toLowerCase();
        roles = roles.stream()
            .filter(role -> role.getRoleId().toLowerCase().contains(roleIdFilter))
            .collect(Collectors.toList());
      }

      // Filter by roleName if provided
      if (filter.hasFilter("roleName")) {
        String roleNameFilter = filter.getStringFilter("roleName").toLowerCase();
        roles = roles.stream()
            .filter(role -> role.getRoleName().toLowerCase().contains(roleNameFilter))
            .collect(Collectors.toList());
      }

      // Apply pagination
      int fromIndex = (page - 1) * size;
      int toIndex = Math.min(fromIndex + size, roles.size());

      List<GetRoleResponse> pageContent = roles.subList(
          Math.min(fromIndex, roles.size()),
          Math.min(toIndex, roles.size())).stream()
          .map(roleMapper::mapFromRoleEntityToGetRoleResponse)
          .collect(Collectors.toList());

      int totalPages = (roles.size() + size - 1) / size;

      log.info("Successfully retrieved {} roles with filters", pageContent.size());

      return PageResponse.<GetRoleResponse>builder()
          .page(page)
          .size(size)
          .totalPages(totalPages)
          .totalElements((long) roles.size())
          .data(pageContent)
          .build();

    } catch (Exception e) {
      log.error("Failed to retrieve roles from database", e);
      throw e;
    }
  }

  @Override
  public GetRoleResponse getRoleById(String roleId) {
    log.info("Fetching role by ID: {}", roleId);

    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> {
          log.error("Role not found with ID: {}", roleId);
          return new RoleNotFoundException();
        });

    log.info("Successfully retrieved role: {}", roleId);
    return roleMapper.mapFromRoleEntityToGetRoleResponse(role);
  }

  @Override
  @Transactional
  public GetRoleResponse createRole(RoleCreationRequest request) {
    log.info("Creating new role with ID: {}", request.getRoleId());

    // Check if role already exists
    if (roleRepository.existsById(request.getRoleId())) {
      log.error("Role already exists with ID: {}", request.getRoleId());
      throw new RoleExistingException();
    }

    // Create new role
    Role role = Role.builder()
        .roleId(request.getRoleId())
        .roleName(request.getRoleName())
        .build();

    role = roleRepository.saveAndFlush(role);
    log.info("Successfully created role: {}", role.getRoleId());

    return roleMapper.mapFromRoleEntityToGetRoleResponse(role);
  }

  @Override
  @Transactional
  public GetRoleResponse updateRole(String roleId, RoleUpdateRequest request) {
    log.info("Updating role with ID: {}", roleId);

    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> {
          log.error("Role not found with ID: {}", roleId);
          return new RoleNotFoundException();
        });

    // Update role name
    role.setRoleName(request.getRoleName());

    role = roleRepository.saveAndFlush(role);
    log.info("Successfully updated role: {}", roleId);

    return roleMapper.mapFromRoleEntityToGetRoleResponse(role);
  }

  @Override
  @Transactional
  public void deleteRole(String roleId) {
    log.info("Deleting role with ID: {}", roleId);

    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> {
          log.error("Role not found with ID: {}", roleId);
          return new RoleNotFoundException();
        });

    roleRepository.delete(role);
    log.info("Successfully deleted role: {}", roleId);
  }
}
