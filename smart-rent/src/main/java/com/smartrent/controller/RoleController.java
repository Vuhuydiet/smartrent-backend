package com.smartrent.controller;

import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.GetRoleResponse;
import com.smartrent.service.role.RoleService;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

  RoleService roleService;

  @GetMapping
  ApiResponse<List<GetRoleResponse>> getAllRoles() {
    List<GetRoleResponse> roles = roleService.getAllRoles();

    return ApiResponse.<List<GetRoleResponse>>builder()
        .data(roles)
        .build();
  }
}
