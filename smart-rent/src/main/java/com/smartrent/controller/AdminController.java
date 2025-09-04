package com.smartrent.controller;

import com.smartrent.config.Constants;
import com.smartrent.controller.dto.request.AdminCreationRequest;
import com.smartrent.controller.dto.response.AdminCreationResponse;
import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.GetAdminResponse;
import com.smartrent.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

  AdminService adminService;

  @PostMapping
  ApiResponse<AdminCreationResponse> createAdmin(@RequestBody @Valid AdminCreationRequest adminCreationRequest) {
    AdminCreationResponse adminCreationResponse = adminService.createAdmin(adminCreationRequest);
    return ApiResponse.<AdminCreationResponse>builder()
        .data(adminCreationResponse)
        .build();
  }

  @GetMapping
  ApiResponse<GetAdminResponse> getAdminById(@RequestHeader(Constants.ADMIN_ID) String id) {
    GetAdminResponse getAdminResponse = adminService.getAdminById(id);
    return ApiResponse.<GetAdminResponse>builder()
        .data(getAdminResponse)
        .build();
  }
}
