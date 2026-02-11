package com.smartrent.controller;

import com.smartrent.dto.request.RoleCreationRequest;
import com.smartrent.dto.request.RoleUpdateRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetRoleResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.service.role.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

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
      description = "Retrieves a paginated list of all available roles in the system. This endpoint is typically used for populating role selection dropdowns in admin interfaces.",
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
                        "data": {
                          "page": 1,
                          "size": 10,
                          "totalElements": 4,
                          "totalPages": 1,
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
  ApiResponse<PageResponse<GetRoleResponse>> getAllRoles(
      @Parameter(description = "Page number (1-indexed)", example = "1")
      @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page", example = "10")
      @RequestParam(defaultValue = "10") int size) {
    PageResponse<GetRoleResponse> roles = roleService.getAllRoles(page, size);

    return ApiResponse.<PageResponse<GetRoleResponse>>builder()
        .data(roles)
        .build();
  }

  @GetMapping("/{roleId}")
  @Operation(
      summary = "Get role by ID",
      description = "Retrieves a specific role by its ID",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Role retrieved successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "roleId": "SA",
                          "roleName": "Super Admin"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "Role not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "4013",
                        "message": "Role not found",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetRoleResponse> getRoleById(
      @Parameter(description = "Role ID", example = "SA", required = true)
      @PathVariable String roleId
  ) {
    GetRoleResponse role = roleService.getRoleById(roleId);
    return ApiResponse.<GetRoleResponse>builder()
        .data(role)
        .build();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create new role",
      description = "Creates a new role in the system",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "201",
          description = "Role created successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "roleId": "MA",
                          "roleName": "Marketing Admin"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "409",
          description = "Role already exists",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Conflict Error",
                  value = """
                      {
                        "code": "3005",
                        "message": "Role already exists",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetRoleResponse> createRole(
      @Valid @RequestBody RoleCreationRequest request
  ) {
    GetRoleResponse role = roleService.createRole(request);
    return ApiResponse.<GetRoleResponse>builder()
        .data(role)
        .build();
  }

  @PutMapping("/{roleId}")
  @Operation(
      summary = "Update role",
      description = "Updates an existing role's information",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Role updated successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class),
              examples = @ExampleObject(
                  name = "Success Response",
                  value = """
                      {
                        "code": "999999",
                        "message": null,
                        "data": {
                          "roleId": "MA",
                          "roleName": "Marketing Administrator"
                        }
                      }
                      """
              )
          )
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "Role not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "4013",
                        "message": "Role not found",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  ApiResponse<GetRoleResponse> updateRole(
      @Parameter(description = "Role ID", example = "MA", required = true)
      @PathVariable String roleId,
      @Valid @RequestBody RoleUpdateRequest request
  ) {
    GetRoleResponse role = roleService.updateRole(roleId, request);
    return ApiResponse.<GetRoleResponse>builder()
        .data(role)
        .build();
  }

  @DeleteMapping("/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete role",
      description = "Deletes a role from the system",
      security = @SecurityRequirement(name = "Bearer Authentication")
  )
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "204",
          description = "Role deleted successfully"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "Role not found",
          content = @Content(
              mediaType = "application/json",
              examples = @ExampleObject(
                  name = "Not Found Error",
                  value = """
                      {
                        "code": "4013",
                        "message": "Role not found",
                        "data": null
                      }
                      """
              )
          )
      )
  })
  void deleteRole(
      @Parameter(description = "Role ID", example = "MA", required = true)
      @PathVariable String roleId
  ) {
    roleService.deleteRole(roleId);
  }
}
