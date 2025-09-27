package com.smartrent.controller;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.service.role.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Role Management", description = "APIs for managing system roles and permissions")
public class RoleController {

  RoleService roleService;

  @GetMapping
  @Operation(
      summary = "Get all available roles",
      description = "Retrieves a list of all available roles in the system. This endpoint is typically used for populating role selection dropdowns in admin interfaces.",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Roles retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": [
                          {
                            "roleId": "ADMIN",
                            "roleName": "Administrator"
                          },
                          {
                            "roleId": "SUPER_ADMIN",
                            "roleName": "Super Administrator"
                          },
                          {
                            "roleId": "USER",
                            "roleName": "Regular User"
                          },
                          {
                            "roleId": "MODERATOR",
                            "roleName": "Content Moderator"
                          }
                        ]
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "401",
          description = "Unauthorized - Invalid or missing authentication token",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Unauthorized Error",
                  value = """
                      {
                        "code": "401001",
                        "message": "UNAUTHORIZED",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "403",
          description = "Forbidden - Insufficient permissions to view roles",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Forbidden Error",
                  value = """
                      {
                        "code": "403001",
                        "message": "INSUFFICIENT_PERMISSIONS",
                        "data": null
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "500",
          description = "Internal server error",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Server Error",
                  value = """
                      {
                        "code": "500001",
                        "message": "INTERNAL_SERVER_ERROR",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<List<GetRoleResponse>> getAllRoles() {
    List<GetRoleResponse> roles = roleService.getAllRoles();

    return ApiResponse.<List<GetRoleResponse>>builder()
        .data(roles)
        .build();
  }
}
